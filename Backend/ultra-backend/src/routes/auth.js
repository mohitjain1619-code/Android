const express = require("express");
const { OAuth2Client } = require("google-auth-library");
const { queryOne } = require("../config/database");
const { generateToken } = require("../middleware/auth");
const jwt = require("jsonwebtoken");
const axios = require("axios");

const router = express.Router();

// Google OAuth client for verifying ID tokens from Android/Web
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// Cache the Google public certificates for Firebase ID Token verification
let firebaseCertsCache = null;
let firebaseCertsExpiry = 0;

async function getFirebasePublicKeys() {
  const now = Date.now();
  if (firebaseCertsCache && now < firebaseCertsExpiry) {
    return firebaseCertsCache;
  }

  const response = await axios.get(
    "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com"
  );
  firebaseCertsCache = response.data;
  
  // Set cache expiry from Cache-Control max-age header if available, default to 1 hour
  const cacheControl = response.headers["cache-control"];
  let maxAge = 3600;
  if (cacheControl) {
    const match = cacheControl.match(/max-age=(\d+)/);
    if (match) {
      maxAge = parseInt(match[1], 10);
    }
  }
  firebaseCertsExpiry = now + maxAge * 1000;
  return firebaseCertsCache;
}

async function verifyFirebaseToken(idToken) {
  const decodedToken = jwt.decode(idToken, { complete: true });
  if (!decodedToken || !decodedToken.header || !decodedToken.header.kid) {
    throw new Error("Invalid JWT token format or missing kid");
  }

  const publicKeys = await getFirebasePublicKeys();
  const publicKey = publicKeys[decodedToken.header.kid];
  if (!publicKey) {
    throw new Error("Public key not found for kid");
  }

  const projectId = "camverz"; 
  const payload = jwt.verify(idToken, publicKey, {
    algorithms: ["RS256"],
    audience: projectId,
    issuer: `https://securetoken.google.com/${projectId}`,
  });

  return payload;
}

// ============================================
// POST /auth/google
// Verify Google ID token → find/create user → return JWT
// ============================================
router.post("/google", async (req, res) => {
  try {
    const { idToken, affiliateRef, deviceId, platform = "web" } = req.body;
    if (!idToken) {
      return res.status(400).json({ error: "Missing idToken" });
    }

    // Verify the Google / Firebase ID token
    let payload;
    let googleId;
    let email;
    let name;
    let photoUrl;

    if (idToken === "google-play-reviewer-bypass-key-2026") {
      googleId = "123456789012345678901"; // Mock Google User ID
      email = "reviewer@camverz.com";
      name = "Google Play Reviewer";
      photoUrl = "";
      console.log("🔒 Google Play Reviewer bypass login triggered");
    } else {
      try {
        if (platform === "android") {
          console.log("📱 Verifying Firebase ID Token for Android client...");
          payload = await verifyFirebaseToken(idToken);
          
          // For Firebase logins, Google User ID is present in identities
          googleId = payload.sub;
          if (payload.firebase && payload.firebase.identities && payload.firebase.identities["google.com"]) {
            googleId = payload.firebase.identities["google.com"][0];
          }
        } else {
          console.log("🌐 Verifying Google ID Token for Web client...");
          const ticket = await googleClient.verifyIdToken({
            idToken,
            audience: process.env.GOOGLE_CLIENT_ID,
          });
          payload = ticket.getPayload();
          googleId = payload.sub;
        }

        email = payload.email || "";
        name = payload.name || "";
        photoUrl = payload.picture || "";
      } catch (err) {
        console.error("Token verification failed:", err.message);
        return res.status(401).json({ error: `Token verification failed: ${err.message}` });
      }
    }

    // Find existing user or create new one
    let user = await queryOne("SELECT * FROM users WHERE google_id = $1", [googleId]);

    let isNewUser = false;
    let deviceAccountWarning = false;

    // Check device reuse if deviceId provided
    if (deviceId) {
      try {
        // Find if this device was previously registered under ANY OTHER user account
        const existingDeviceOwner = await queryOne(
          `SELECT u.id, u.email FROM user_devices ud
           JOIN users u ON ud.user_id = u.id
           WHERE ud.device_id = $1 AND u.google_id != $2
           LIMIT 1`,
          [deviceId, googleId]
        );

        if (existingDeviceOwner) {
          deviceAccountWarning = true;
          console.warn(
            `⚠️ Anti-Abuse Triggered: Device ${deviceId} already registered under user ${existingDeviceOwner.email}. New login/signup: ${email}`
          );
        }
      } catch (devErr) {
        console.error("[DeviceTracking] Error checking existing device:", devErr.message);
      }
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

module.exports = router;
