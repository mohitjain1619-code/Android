const express = require("express");
const { OAuth2Client } = require("google-auth-library");
const { queryOne } = require("../config/database");
const { generateToken } = require("../middleware/auth");

const router = express.Router();

// Google OAuth client for verifying ID tokens from Android/Web
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

// ============================================
// POST /auth/google
// Verify Google ID token → find/create user → return JWT
// ============================================
router.post("/google", async (req, res) => {
  try {
    const { idToken } = req.body;
    if (!idToken) {
      return res.status(400).json({ error: "Missing idToken" });
    }

    // Verify the Google ID token
    let payload;
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

    const googleId = payload.sub;
    const email = payload.email || "";
    const name = payload.name || "";
    const photoUrl = payload.picture || "";

    // Find existing user or create new one
    let user = await queryOne("SELECT * FROM users WHERE google_id = $1", [googleId]);

    let isNewUser = false;
    if (!user) {
      // Create new user
      isNewUser = true;
      user = await queryOne(
        `INSERT INTO users (google_id, email, name, photo_url, custom_id)
         VALUES ($1, $2, $3, $4, $5)
         RETURNING *`,
        [googleId, email, name, photoUrl, googleId.substring(0, 8)]
      );
      console.log(`✅ New user created: ${user.id} (${email})`);
    } else {
      console.log(`✅ Existing user logged in: ${user.id} (${email})`);
    }

    // Generate JWT
    const token = generateToken(user);

    return res.json({
      ok: true,
      token,
      isNewUser,
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
