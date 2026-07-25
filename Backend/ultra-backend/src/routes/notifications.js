const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();
router.use(requireAuth);

// ============================================
// GET /notifications — Get user's notifications
// ============================================
router.get("/", async (req, res) => {
  try {
    const { limit = 50, offset = 0 } = req.query;

    const notifications = await queryMany(
      `SELECT n.*, 
              u.name as triggering_user_name, 
              u.avatar as triggering_user_avatar,
              u.photo_url as triggering_user_photo_url,
              p.text as post_text
       FROM notifications n
       LEFT JOIN users u ON u.id = n.triggering_user_id
       LEFT JOIN posts p ON p.id = n.post_id
       WHERE n.user_id = $1
       ORDER BY n.created_at DESC
       LIMIT $2 OFFSET $3`,
      [req.user.userId, parseInt(limit), parseInt(offset)]
    );

    const unreadCount = await queryOne(
      "SELECT COUNT(*) as count FROM notifications WHERE user_id = $1 AND read = false",
      [req.user.userId]
    );

    return res.json({
      ok: true,
      notifications: notifications.map((n) => ({
        id: n.id,
        type: n.type,
        read: n.read,
        triggeringUser: n.triggering_user_id ? {
          id: n.triggering_user_id,
          name: n.triggering_user_name,
          avatar: n.triggering_user_avatar,
          photoUrl: n.triggering_user_photo_url,
        } : null,
        postId: n.post_id,
        postText: n.post_text,
        commentId: n.comment_id,
        createdAt: n.created_at,
      })),
      unreadCount: parseInt(unreadCount.count),
    });
  } catch (err) {
    console.error("Get notifications error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// PUT /notifications/:id/read — Mark notification as read
// ============================================
router.put("/:id/read", async (req, res) => {
  try {
    await query(
      "UPDATE notifications SET read = true WHERE id = $1 AND user_id = $2",
      [req.params.id, req.user.userId]
    );
    return res.json({ ok: true });
  } catch (err) {
    console.error("Mark read error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// PUT /notifications/read-all — Mark all as read
// ============================================
router.put("/read-all", async (req, res) => {
  try {
    await query(
      "UPDATE notifications SET read = true WHERE user_id = $1 AND read = false",
      [req.user.userId]
    );
    return res.json({ ok: true });
  } catch (err) {
    console.error("Mark all read error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /notifications/unread-count — Get unread count only
// ============================================
router.get("/unread-count", async (req, res) => {
  try {
    const result = await queryOne(
      "SELECT COUNT(*) as count FROM notifications WHERE user_id = $1 AND read = false",
      [req.user.userId]
    );
    return res.json({ ok: true, count: parseInt(result.count) });
  } catch (err) {
    console.error("Unread count error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

module.exports = router;
