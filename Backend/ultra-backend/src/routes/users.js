const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();

const isValidUUID = (str) =>
  typeof str === "string" &&
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(str);

// All routes require authentication
router.use(requireAuth);

// ============================================
// GET /users/me — Get current user profile
// ============================================
router.get("/me", async (req, res) => {
  try {
    let user = await queryOne("SELECT * FROM users WHERE id = $1", [req.user.userId]);
    if (!user && req.user.email) {
      user = await queryOne("SELECT * FROM users WHERE email = $1", [req.user.email]);
    }
    if (!user) {
      return res.json({
        ok: true,
        user: {
          id: req.user.userId,
          name: req.user.email ? req.user.email.split("@")[0] : "User",
          email: req.user.email || "",
          avatar: "av1",
          photoUrl: null,
          gender: "male",
          sexPreference: "Straight",
          verified: false
        },
        followersCount: 0,
        followingCount: 0
      });
    }

    const followers = await queryOne("SELECT COUNT(*) as count FROM follows WHERE following_id = $1", [user.id]);
    const following = await queryOne("SELECT COUNT(*) as count FROM follows WHERE follower_id = $1", [user.id]);

    return res.json({
      ok: true,
      user: formatUser(user),
      followersCount: parseInt((followers && followers.count) || 0),
      followingCount: parseInt((following && following.count) || 0),
    });
  } catch (err) {
    console.error("Get me error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /users/:id — Get user profile by ID
// ============================================
router.get("/:id", async (req, res) => {
  try {
    let user;
    if (isValidUUID(req.params.id)) {
      user = await queryOne("SELECT * FROM users WHERE id = $1", [req.params.id]);
    } else {
      user = await queryOne("SELECT * FROM users WHERE custom_id = $1 OR google_id = $1", [req.params.id]);
    }
    if (!user && req.params.id === "me") {
      user = await queryOne("SELECT * FROM users WHERE id = $1", [req.user.userId]);
    }
    if (!user) {
      return res.json({
        ok: true,
        user: {
          id: req.params.id,
          name: "User",
          email: "",
          avatar: "av1",
          photoUrl: null,
          gender: "male",
          sexPreference: "Straight",
          verified: false,
          followersCount: 0,
          followingCount: 0,
          isFollowedByMe: false,
          isBlocked: false,
          isBlockedByOther: false,
          friendshipStatus: "none",
          friendshipRequestId: null
        }
      });
    }

    const followers = await queryOne("SELECT COUNT(*) as count FROM follows WHERE following_id = $1", [user.id]);
    const following = await queryOne("SELECT COUNT(*) as count FROM follows WHERE follower_id = $1", [user.id]);

    // Check if current user follows this user
    const isFollowing = await queryOne(
      "SELECT 1 FROM follows WHERE follower_id = $1 AND following_id = $2",
      [req.user.userId, user.id]
    );

    // Check block status
    const isBlocked = await queryOne(
      "SELECT 1 FROM blocked_users WHERE blocker_id = $1 AND blocked_id = $2",
      [req.user.userId, user.id]
    );
    const isBlockedByOther = await queryOne(
      "SELECT 1 FROM blocked_users WHERE blocker_id = $1 AND blocked_id = $2",
      [user.id, req.user.userId]
    );

    // Check friendship status
    const sent = await queryOne(
      "SELECT * FROM friend_requests WHERE from_user_id = $1 AND to_user_id = $2",
      [req.user.userId, user.id]
    );
    const received = await queryOne(
      "SELECT * FROM friend_requests WHERE from_user_id = $1 AND to_user_id = $2",
      [user.id, req.user.userId]
    );
    let friendshipStatus = "none";
    let friendshipRequestId = null;
    if (sent) {
      friendshipStatus = sent.status === "accepted" ? "friends" : "sent";
      friendshipRequestId = sent.id;
    } else if (received) {
      friendshipStatus = received.status === "accepted" ? "friends" : "received";
      friendshipRequestId = received.id;
    }

    return res.json({
      ok: true,
      user: {
        ...formatUser(user),
        followersCount: parseInt(followers.count),
        followingCount: parseInt(following.count),
        isFollowedByMe: !!isFollowing,
        isBlocked: !!isBlocked,
        isBlockedByOther: !!isBlockedByOther,
        friendshipStatus,
        friendshipRequestId,
      }
    });
  } catch (err) {
    console.error("Get user error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// PUT /users/me — Update current user profile
// ============================================
router.put("/me", async (req, res) => {
  try {
    const allowedFields = ["name", "gender", "avatar", "bio", "dob", "city", "custom_id", "verified", "sex_preference"];
    const updates = {};
    for (const field of allowedFields) {
      // Support camelCase from client
      const camelField = field.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
      if (req.body[field] !== undefined) updates[field] = req.body[field];
      else if (req.body[camelField] !== undefined) updates[field] = req.body[camelField];
    }

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({ error: "No valid fields to update" });
    }

    // Ensure user row exists in DB before updating
    let userExists = await queryOne("SELECT id FROM users WHERE id = $1", [req.user.userId]);
    if (!userExists) {
      const email = req.user.email || `${req.user.userId}@camverz.com`;
      await query(
        `INSERT INTO users (id, email, name, avatar) VALUES ($1, $2, $3, $4) ON CONFLICT (id) DO NOTHING`,
        [req.user.userId, email, email.split("@")[0], "av1"]
      );
    }

    // Auto-verify male users, and reset verification for female users if they change gender
    if (updates.gender && updates.gender.toLowerCase() === "male") {
      updates.verified = true;
    } else if (updates.gender && updates.gender.toLowerCase() === "female") {
      // Only set to false if they are changing their gender to female and verified is not explicitly passed as true
      if (req.body.verified === undefined) {
        updates.verified = false;
      }
    }

    const setClauses = Object.keys(updates).map((k, i) => `${k} = $${i + 2}`);
    const values = [req.user.userId, ...Object.values(updates)];

    const user = await queryOne(
      `UPDATE users SET ${setClauses.join(", ")} WHERE id = $1 RETURNING *`,
      values
    );

    return res.json({ ok: true, user: formatUser(user) });
  } catch (err) {
    console.error("Update user error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /users/:id/follow — Follow a user
// ============================================
router.post("/:id/follow", async (req, res) => {
  try {
    let targetUser;
    if (isValidUUID(req.params.id)) {
      targetUser = await queryOne("SELECT id FROM users WHERE id = $1", [req.params.id]);
    } else {
      targetUser = await queryOne("SELECT id FROM users WHERE custom_id = $1 OR google_id = $1", [req.params.id]);
    }
    if (!targetUser) return res.status(404).json({ error: "User not found" });

    const targetId = targetUser.id;
    if (targetId === req.user.userId) {
      return res.status(400).json({ error: "Cannot follow yourself" });
    }

    await query(
      `INSERT INTO follows (follower_id, following_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
      [req.user.userId, targetId]
    );

    // Create notification
    await query(
      `INSERT INTO notifications (user_id, type, triggering_user_id)
       VALUES ($1, 'follow', $2)`,
      [targetId, req.user.userId]
    );

    return res.json({ ok: true });
  } catch (err) {
    console.error("Follow error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// DELETE /users/:id/follow — Unfollow a user
// ============================================
router.delete("/:id/follow", async (req, res) => {
  try {
    await query("DELETE FROM follows WHERE follower_id = $1 AND following_id = $2", [
      req.user.userId,
      req.params.id,
    ]);
    return res.json({ ok: true });
  } catch (err) {
    console.error("Unfollow error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /users/:id/followers — Get followers list
// ============================================
router.get("/:id/followers", async (req, res) => {
  try {
    const followers = await queryMany(
      `SELECT u.* FROM users u
       JOIN follows f ON f.follower_id = u.id
       WHERE f.following_id = $1
       ORDER BY f.created_at DESC`,
      [req.params.id]
    );
    return res.json({ ok: true, users: followers.map(formatUser) });
  } catch (err) {
    console.error("Get followers error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /users/:id/following — Get following list
// ============================================
router.get("/:id/following", async (req, res) => {
  try {
    const following = await queryMany(
      `SELECT u.* FROM users u
       JOIN follows f ON f.following_id = u.id
       WHERE f.follower_id = $1
       ORDER BY f.created_at DESC`,
      [req.params.id]
    );
    return res.json({ ok: true, users: following.map(formatUser) });
  } catch (err) {
    console.error("Get following error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /users/:id/block — Block a user
// ============================================
router.post("/:id/block", async (req, res) => {
  try {
    await query(
      `INSERT INTO blocked_users (blocker_id, blocked_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
      [req.user.userId, req.params.id]
    );
    // Also unfollow both directions
    await query("DELETE FROM follows WHERE (follower_id = $1 AND following_id = $2) OR (follower_id = $2 AND following_id = $1)", [
      req.user.userId,
      req.params.id,
    ]);
    return res.json({ ok: true });
  } catch (err) {
    console.error("Block error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// DELETE /users/:id/block — Unblock a user
// ============================================
router.delete("/:id/block", async (req, res) => {
  try {
    await query("DELETE FROM blocked_users WHERE blocker_id = $1 AND blocked_id = $2", [
      req.user.userId,
      req.params.id,
    ]);
    return res.json({ ok: true });
  } catch (err) {
    console.error("Unblock error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /users/me/blocked — Get blocked users list
// ============================================
router.get("/me/blocked", async (req, res) => {
  try {
    const blocked = await queryMany(
      `SELECT u.* FROM users u
       JOIN blocked_users b ON b.blocked_id = u.id
       WHERE b.blocker_id = $1`,
      [req.user.userId]
    );
    return res.json({ ok: true, users: blocked.map(formatUser) });
  } catch (err) {
    console.error("Get blocked error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// DELETE /users/me — Delete account (cascade cleanup via PostgreSQL)
// ============================================
router.delete("/me", async (req, res) => {
  try {
    // PostgreSQL ON DELETE CASCADE handles all related data automatically
    await query("DELETE FROM users WHERE id = $1", [req.user.userId]);
    console.log(`🧹 User ${req.user.userId} deleted with cascade cleanup`);
    return res.json({ ok: true });
  } catch (err) {
    console.error("Delete account error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /users/:id/report — Report a user
// ============================================
router.post("/:id/report", async (req, res) => {
  try {
    const { reason } = req.body;
    await query(
      `INSERT INTO reports (reporter_id, reported_id, reason) VALUES ($1, $2, $3)`,
      [req.user.userId, req.params.id, reason || ""]
    );
    return res.json({ ok: true });
  } catch (err) {
    console.error("Report error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// Helper: format user object for API response
function formatUser(user) {
  if (!user) return null;
  return {
    id: user.id,
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
    planName: user.plan_name,
    planIsAdFree: user.plan_is_ad_free,
    planExpiresAt: user.plan_expires_at,
    planStartedAt: user.plan_started_at,
    sexPreference: user.sex_preference || "Straight",
    subscription: {
      planName: user.plan_name,
      isAdFree: user.plan_is_ad_free,
      expiresAt: user.plan_expires_at,
      startedAt: user.plan_started_at
    }
  };
}

module.exports = router;
