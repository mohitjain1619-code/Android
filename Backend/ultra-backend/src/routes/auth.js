const express = require("express");
const { OAuth2Client } = require("google-auth-library");
const { query, queryOne } = require("../config/database");
const { generateToken } = require("../middleware/auth");
const redisModule = require("../config/redis");

const router = express.Router();

// Google OAuth client for verifying ID tokens from Android/Web
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

const COUNTRY_NAME_MAP = {
  IN: "India", US: "United States", CA: "Canada", GB: "United Kingdom", AU: "Australia",
  DE: "Germany", FR: "France", AE: "UAE", NP: "Nepal", BD: "Bangladesh", PK: "Pakistan",
  SG: "Singapore", MY: "Malaysia", ID: "Indonesia", BR: "Brazil", MX: "Mexico", ES: "Spain",
  IT: "Italy", NL: "Netherlands", RU: "Russia", JP: "Japan", KR: "South Korea", CN: "China"
};

function getCountryFromReq(req) {
  const cfCountry = (req.headers["cf-ipcountry"] || req.headers["x-country"] || req.headers["x-geoip-country"] || "").trim().toUpperCase();
  if (cfCountry && cfCountry !== "XX" && cfCountry !== "T1") {
    return COUNTRY_NAME_MAP[cfCountry] || cfCountry;
  }
  const bodyCountry = (req.body && (req.body.countryCode || req.body.country) || "").trim().toUpperCase();
  if (bodyCountry && bodyCountry.length >= 2) {
    return COUNTRY_NAME_MAP[bodyCountry] || bodyCountry;
  }
  return null;
}

// ============================================
// POST /auth/google
// Verify Google ID token → find/create user → return JWT
// ============================================
router.post("/google", async (req, res) => {
  try {
    const { idToken, affiliateRef, ref, deviceId, platform = "web", deviceEmails } = req.body;
    if (!idToken) {
      return res.status(400).json({ error: "Missing idToken" });
    }

    // Detect country from request headers (Cloudflare IP Geo)
    const detectedCountry = getCountryFromReq(req);

    // Verify the Google ID token
    let payload;
    if (idToken === "google-play-reviewer-bypass-key-2026") {
      payload = {
        sub: "123456789012345678901", // Mock Google User ID
        email: "reviewer@camverz.com",
        name: "Google Play Reviewer",
        picture: "",
      };
      console.log("🔒 Google Play Reviewer bypass login triggered");
    } else {
      try {
        const allowedAudiences = [
          process.env.GOOGLE_CLIENT_ID,
          "839315244344-enkkm3rcbebdi6h0djlnu2vtqgviq5mn.apps.googleusercontent.com",
          "973141718708-hjb3sgbdng8k7osid7hn74h8moscr1nc.apps.googleusercontent.com"
        ].filter(Boolean);
        const ticket = await googleClient.verifyIdToken({
          idToken,
          audience: allowedAudiences,
        });
        payload = ticket.getPayload();
      } catch (err) {
        console.error("Google token verification failed:", err.message);
        return res.status(401).json({ error: "Invalid Google ID token" });
      }
    }

    const googleId = payload.sub;
    const email = payload.email || "";
    const name = payload.name || "";
    const photoUrl = payload.picture || "";

    // 1. Device Account Binding Check:
    // Strictly 1 physical device (deviceId) can only log into 1 account.
    // If deviceId is already linked to a different account, BLOCK login attempt on the same device!
    if (deviceId) {
      try {
        const clientIp = (req.headers["x-forwarded-for"] || req.socket.remoteAddress || "").split(",")[0].trim();

        const existingDeviceOwner = await queryOne(
          `SELECT u.id, u.email FROM user_devices ud
           JOIN users u ON ud.user_id = u.id
           WHERE ud.device_id = $1 
           AND LOWER(u.email) != LOWER($2)
           ORDER BY ud.first_seen_at ASC
           LIMIT 1`,
          [deviceId, email]
        );

        if (existingDeviceOwner) {
          console.log(
            `⛔ [DeviceBlock] Device ${deviceId} (IP: ${clientIp}) is already linked to ${existingDeviceOwner.email}. Blocking login attempt for: ${email}`
          );
          return res.status(403).json({
            ok: false,
            error: "device_bound",
            message: `This device is already linked to another account (${existingDeviceOwner.email}). Multiple account logins on the same device are not allowed.`
          });
        }
      } catch (devErr) {
        console.error("[DeviceTracking] Error checking existing device:", devErr.message);
      }
    }

    // Find existing user by google_id or email
    let user = await queryOne(
      "SELECT * FROM users WHERE google_id = $1 OR (email = $2 AND email != '')",
      [googleId, email]
    );

    let isNewUser = false;

    if (user) {
      if (!user.google_id || user.photo_url !== photoUrl || (detectedCountry && user.country !== detectedCountry)) {
        await query(
          "UPDATE users SET google_id = $1, photo_url = COALESCE(NULLIF($2, ''), photo_url), country = COALESCE($3, country) WHERE id = $4",
          [googleId, photoUrl, detectedCountry || null, user.id]
        );
        if (detectedCountry) user.country = detectedCountry;
      }
      console.log(`✅ Existing user logged in: ${user.id} (${email}) | Country: ${user.country || 'Global'}`);
    } else {
      // Create new user
      isNewUser = true;

      user = await queryOne(
        `INSERT INTO users (google_id, email, name, photo_url, custom_id, has_free_trial, country)
         VALUES ($1, $2, $3, $4, $5, true, $6)
         ON CONFLICT (email) DO UPDATE SET google_id = EXCLUDED.google_id, country = COALESCE(users.country, EXCLUDED.country)
         RETURNING *`,
        [googleId, email, name, photoUrl, googleId.substring(0, 8), detectedCountry || null]
      );
      console.log(`✅ New user created: ${user.id} (${email}) | Country: ${user.country || 'Global'}`);
    }

      // Track affiliate signup if referred
      const finalRef = affiliateRef || ref || req.body.refCode;
      if (finalRef) {
        try {
          const refCode = String(finalRef).trim().toUpperCase();
          const aff = await queryOne(
            "SELECT * FROM affiliates WHERE UPPER(code) = $1",
            [refCode]
          );
          if (aff) {
            const existingSignup = await queryOne(
              "SELECT * FROM affiliate_signups WHERE referred_user_id = $1",
              [user.id]
            );
            if (!existingSignup) {
              await queryOne(
                `INSERT INTO affiliate_signups (affiliate_id, referred_user_id, ref_code_used)
                 VALUES ($1, $2, $3)`,
                [aff.id, user.id, refCode]
              );
              console.log(`[Affiliate] Referral Signup: User ${user.id} referred by code ${refCode}`);
            }
          }
        } catch (affErr) {
          console.error("[Affiliate] Signup attribution failed:", affErr);
        }
      }

    // Record / Update device tracking mapping
    if (user) {
      try {
        const clientIp = (req.headers["x-forwarded-for"] || req.socket.remoteAddress || "").split(",")[0].trim();
        const userAgent = req.headers["user-agent"] || "";
        const isMobileAgent = userAgent.toLowerCase().includes("okhttp") || userAgent.toLowerCase().includes("android");
        const activePlatform = (platform && platform.toLowerCase() !== "web") ? platform.toLowerCase() : (isMobileAgent ? "android" : "web");
        const activeDeviceId = deviceId || `${activePlatform}-${user.id}`;

        await queryOne(
          `INSERT INTO user_devices (device_id, user_id, platform, ip_address, user_agent, last_seen_at)
           VALUES ($1, $2, $3, $4, $5, NOW())
           ON CONFLICT (device_id, user_id)
           DO UPDATE SET last_seen_at = NOW(), ip_address = EXCLUDED.ip_address, user_agent = EXCLUDED.user_agent, platform = EXCLUDED.platform`,
          [activeDeviceId, user.id, activePlatform, clientIp, userAgent]
        );
      } catch (devSaveErr) {
        console.error("[DeviceTracking] Error saving device mapping:", devSaveErr.message);
      }
    }

    // Generate JWT
    const token = generateToken(user);

    return res.json({
      ok: true,
      token,
      isNewUser,
      deviceAccountWarning,
      hasFreeTrial: user.has_free_trial !== false,
      user: {
        id: user.id,
        googleId: user.google_id,
        email: user.email,
        name: user.name,
        gender: user.gender,
        verified: user.verified,
        avatar: user.avatar,
        bio: user.bio,
        dob: user.dob,
        city: user.city,
        customId: user.custom_id,
        photoUrl: user.photo_url,
        sexPreference: user.sex_preference || null,
        sex_preference: user.sex_preference || null,
        hasFreeTrial: user.has_free_trial !== false,
        createdAt: user.created_at,
      },
    });
  } catch (err) {
    console.error("Auth error:", err);
    return res.status(500).json({ 
      error: "Authentication service temporary issue. Please try again.", 
      details: err.message 
    });
  }
});

// ============================================
// POST /auth/refresh
// Refresh JWT (user sends current valid token)
// ============================================
router.post("/refresh", async (req, res) => {
  try {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
    if (!token) {
      return res.status(401).json({ error: "Missing token" });
    }

    const { verifyToken } = require("../middleware/auth");
    const decoded = verifyToken(token);

    const user = await queryOne("SELECT * FROM users WHERE id = $1", [decoded.userId]);
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }

    const newToken = generateToken(user);
    return res.json({ ok: true, token: newToken });
  } catch (err) {
    return res.status(401).json({ error: "Invalid token" });
  }
});

// ============================================
// GET /auth/autologin
// One-time token login → generates JWT → redirects to Web client
// ============================================
router.get("/autologin", async (req, res) => {
  try {
    const { token, redirect = "profile" } = req.query;
    if (!token) {
      return res.status(400).send("Missing autologin token");
    }

    // Retrieve user from Redis using the token
    const userId = await redisModule.getAutologinUser(token);
    if (!userId) {
      return res.status(401).send("Invalid or expired autologin token");
    }

    // Find user in database
    const user = await queryOne("SELECT * FROM users WHERE id = $1", [userId]);
    if (!user) {
      return res.status(404).send("User not found");
    }

    // Generate JWT token
    const jwtToken = generateToken(user);

    // Redirect to front-end auto-login page
    const frontendUrl = process.env.FRONTEND_URL || "https://camverz-nine.vercel.app";
    return res.redirect(`${frontendUrl}/auth/autologin?token=${jwtToken}&redirect=${redirect}`);
  } catch (err) {
    console.error("Autologin redirect error:", err);
    return res.status(500).send("Internal server error during autologin redirect");
  }
});

module.exports = router;
