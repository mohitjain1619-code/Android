const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();
router.use(requireAuth);

// ============================================
// POST /friends/request — Send friend request
// ============================================
router.post("/request", async (req, res) => {
  try {
    const { targetUserId } = req.body;
    if (!targetUserId) return res.status(400).json({ error: "Missing targetUserId" });
    if (targetUserId === req.user.userId) return res.status(400).json({ error: "Cannot friend yourself" });

    // Check if already exists
    const existing = await queryOne(
      "SELECT * FROM friend_requests WHERE from_user_id = $1 AND to_user_id = $2",
      [req.user.userId, targetUserId]
    );
    if (existing) {
      return res.status(409).json({ error: "Request already sent", status: existing.status });
    }

    const request = await queryOne(
      `INSERT INTO friend_requests (from_user_id, to_user_id) VALUES ($1, $2) RETURNING *`,
      [req.user.userId, targetUserId]
    );

    // Create notification
    await query(
      `INSERT INTO notifications (user_id, type, triggering_user_id) VALUES ($1, 'friend_request', $2)`,
      [targetUserId, req.user.userId]
    );

    return res.json({ ok: true, request: formatRequest(request) });
  } catch (err) {
    console.error("Send friend request error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// PUT /friends/request/:id/accept — Accept friend request
// ============================================
router.put("/request/:id/accept", async (req, res) => {
  try {
    const request = await queryOne(
      "SELECT * FROM friend_requests WHERE id = $1 AND to_user_id = $2 AND status = 'pending'",
      [req.params.id, req.user.userId]
    );
    if (!request) return res.status(404).json({ error: "Request not found" });

    // Update status
    await query("UPDATE friend_requests SET status = 'accepted' WHERE id = $1", [req.params.id]);

    // Auto-follow both directions
    await query(
      "INSERT INTO follows (follower_id, following_id) VALUES ($1, $2) ON CONFLICT DO NOTHING",
      [request.from_user_id, request.to_user_id]
    );
    await query(
      "INSERT INTO follows (follower_id, following_id) VALUES ($1, $2) ON CONFLICT DO NOTHING",
      [request.to_user_id, request.from_user_id]
    );

    // Notification
    await query(
      `INSERT INTO notifications (user_id, type, triggering_user_id) VALUES ($1, 'friend_accepted', $2)`,
      [request.from_user_id, req.user.userId]
    );

    return res.json({ ok: true });
  } catch (err) {
    console.error("Accept friend request error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// PUT /friends/request/:id/reject — Reject friend request
// ============================================
router.put("/request/:id/reject", async (req, res) => {
  try {
    const request = await queryOne(
      "SELECT * FROM friend_requests WHERE id = $1 AND to_user_id = $2 AND status = 'pending'",
      [req.params.id, req.user.userId]
    );
    if (!request) return res.status(404).json({ error: "Request not found" });

    await query("UPDATE friend_requests SET status = 'rejected' WHERE id = $1", [req.params.id]);

    return res.json({ ok: true });
  } catch (err) {
    console.error("Reject friend request error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /friends/requests — Get pending friend requests
// ============================================
router.get("/requests", async (req, res) => {
  try {
    const { type = "received" } = req.query;

    let requests;
    if (type === "sent") {
      requests = await queryMany(
        `SELECT fr.*, u.name, u.avatar, u.photo_url, u.gender, u.verified
         FROM friend_requests fr
         JOIN users u ON u.id = fr.to_user_id
         WHERE fr.from_user_id = $1 AND fr.status = 'pending'
         ORDER BY fr.created_at DESC`,
        [req.user.userId]
      );
    } else {
      requests = await queryMany(
        `SELECT fr.*, u.name, u.avatar, u.photo_url, u.gender, u.verified
         FROM friend_requests fr
         JOIN users u ON u.id = fr.from_user_id
         WHERE fr.to_user_id = $1 AND fr.status = 'pending'
         ORDER BY fr.created_at DESC`,
        [req.user.userId]
      );
    }

    return res.json({
      ok: true,
      requests: requests.map((r) => ({
        ...formatRequest(r),
        user: {
          id: type === "sent" ? r.to_user_id : r.from_user_id,
          name: r.name,
          avatar: r.avatar,
          photoUrl: r.photo_url,
          gender: r.gender,
          verified: r.verified,
        },
      })),
    });
  } catch (err) {
    console.error("Get friend requests error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /friends/status/:userId — Check friend status with a user
// ============================================
router.get("/status/:userId", async (req, res) => {
  try {
    const targetId = req.params.userId;

    // Check if request exists in either direction
    const sent = await queryOne(
      "SELECT * FROM friend_requests WHERE from_user_id = $1 AND to_user_id = $2",
      [req.user.userId, targetId]
    );
    const received = await queryOne(
      "SELECT * FROM friend_requests WHERE from_user_id = $1 AND to_user_id = $2",
      [targetId, req.user.userId]
    );

    let status = "none";
    let requestId = null;

    if (sent) {
      status = sent.status === "accepted" ? "friends" : "sent";
      requestId = sent.id;
    } else if (received) {
      status = received.status === "accepted" ? "friends" : "received";
      requestId = received.id;
    }

    return res.json({ ok: true, status, requestId });
  } catch (err) {
    console.error("Friend status error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

function formatRequest(r) {
  return {
    id: r.id,
    fromUserId: r.from_user_id,
    toUserId: r.to_user_id,
    status: r.status,
    createdAt: r.created_at,
  };
}

module.exports = router;
