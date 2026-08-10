const express = require("express");
const { OAuth2Client } = require("google-auth-library");
const { queryOne } = require("../config/database");
const { generateToken } = require("../middleware/auth");
const redisModule = require("../config/redis");

const router = express.Router();

// Google OAuth client for verifying ID tokens from Android/Web
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// ============================================
// POST /auth/google
// Verify Google ID token → find/create user → return JWT
// ============================================
router.post("/google", async (req, res) => {
  try {
    const { idToken, affiliateRef, deviceId, platform = "web", deviceEmails } = req.body;
    if (!idToken) {
      return res.status(400).json({ error: "Missing idToken" });
    }

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
        const ticket = await googleClient.verifyIdToken({
          idToken,
          audience: process.env.GOOGLE_CLIENT_ID,
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

    // 1. Strict Device Account Binding (Block if device is already registered to a different account)
    if (deviceId) {
      try {
        const existingDeviceOwner = await queryOne(
          `SELECT u.id, u.email FROM user_devices ud
           JOIN users u ON ud.user_id = u.id
           WHERE ud.device_id = $1 AND u.google_id != $2
           LIMIT 1`,
          [deviceId, googleId]
        );

        if (existingDeviceOwner) {
          console.warn(
            `⚠️ Anti-Abuse Blocked: Device ${deviceId} is already linked to ${existingDeviceOwner.email}. Attempted login: ${email}`
          );
          return res.status(403).json({
            error: "device_bound",
            message: `This device is already linked to another account (${existingDeviceOwner.email}). Please log in using that account.`
          });
        }
      } catch (devErr) {
        console.error("[DeviceTracking] Error checking existing device:", devErr.message);
      }
    }

    // 2. Secondary Device Google Accounts Check (Android only)
    if (Array.isArray(deviceEmails) && deviceEmails.length > 0) {
      try {
        const cleanEmails = deviceEmails
          .map(e => (e || "").trim().toLowerCase())
          .filter(e => e.length > 0 && e !== email.toLowerCase());

        if (cleanEmails.length > 0) {
          const placeholders = cleanEmails.map((_, idx) => `$${idx + 1}`).join(", ");
          const matchedUser = await queryOne(
            `SELECT email FROM users WHERE LOWER(email) IN (${placeholders}) LIMIT 1`,
            cleanEmails
          );

          if (matchedUser) {
            console.warn(
              `⚠️ Anti-Abuse Blocked: Device has registered email ${matchedUser.email} in accounts list. Blocked login for: ${email}`
            );
            return res.status(403).json({
              error: "device_emails_bound",
              message: `This device already has a registered account under ${matchedUser.email}. Please sign in with that email.`
            });
          }
        }
      } catch (emailCheckErr) {
        console.error("[DeviceTracking] Error checking device emails:", emailCheckErr.message);
      }
    }

    // Find existing user or create new one
    let user = await queryOne("SELECT * FROM users WHERE google_id = $1", [googleId]);

    let isNewUser = false;
    let deviceAccountWarning = false;

    // Check device reuse warning status for tracking
    if (deviceId) {
      try {
        const existingDeviceOwner = await queryOne(
          `SELECT u.id, u.email FROM user_devices ud
           JOIN users u ON ud.user_id = u.id
           WHERE ud.device_id = $1 AND u.google_id != $2
           LIMIT 1`,
          [deviceId, googleId]
        );
        if (existingDeviceOwner) {
          deviceAccountWarning = true;
        }
      } catch (e) {}
    }

    if (!user) {
      // Create new user (If device was reused, deny free trial)
      isNewUser = true;
      const initialFreeTrial = !deviceAccountWarning;

      user = await queryOne(
        `INSERT INTO users (google_id, email, name, photo_url, custom_id, has_free_trial)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING *`,
        [googleId, email, name, photoUrl, googleId.substring(0, 8), initialFreeTrial]
      );
      console.log(`✅ New user created: ${user.id} (${email}) | FreeTrial: ${initialFreeTrial}`);

      // Track affiliate signup if referred
      if (affiliateRef) {
        try {
          const refCode = affiliateRef.trim().toUpperCase();
          const aff = await queryOne(
            "SELECT * FROM affiliates WHERE code = $1 AND status = 'approved'",
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
    } else {
      console.log(`✅ Existing user logged in: ${user.id} (${email})`);
    }

    // Record / Update device tracking mapping
    if (deviceId && user) {
      try {
        const clientIp = req.headers["x-forwarded-for"] || req.socket.remoteAddress || "";
        const userAgent = req.headers["user-agent"] || "";

        await queryOne(
          `INSERT INTO user_devices (device_id, user_id, platform, ip_address, user_agent, last_seen_at)
           VALUES ($1, $2, $3, $4, $5, NOW())
           ON CONFLICT (device_id, user_id)
           DO UPDATE SET last_seen_at = NOW(), ip_address = EXCLUDED.ip_address, user_agent = EXCLUDED.user_agent`,
          [deviceId, user.id, platform, clientIp, userAgent]
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
        hasFreeTrial: user.has_free_trial !== false,
        createdAt: user.created_at,
      },
    });
  } catch (err) {
    console.error("Auth error:", err);
    return res.status(500).json({ error: "Internal error", details: err.message });
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
