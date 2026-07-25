const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();
router.use(requireAuth);

// ============================================
// GET /chats — Get user's conversations (inbox)
// ============================================
router.get("/", async (req, res) => {
  try {
    const chats = await queryMany(
      `SELECT c.*,
              CASE WHEN c.user1_id = $1 THEN c.user2_id ELSE c.user1_id END as other_user_id,
              CASE WHEN c.user1_id = $1 THEN c.user1_deleted ELSE c.user2_deleted END as deleted_by_me,
              (SELECT COUNT(*) FROM messages m WHERE m.chat_id = c.id AND m.sender_id != $1 AND m.read = false) as unread_count,
              (SELECT m.text FROM messages m WHERE m.chat_id = c.id ORDER BY m.created_at DESC LIMIT 1) as last_message,
              (SELECT m.created_at FROM messages m WHERE m.chat_id = c.id ORDER BY m.created_at DESC LIMIT 1) as last_message_at
       FROM chats c
       WHERE (c.user1_id = $1 OR c.user2_id = $1)
         AND CASE WHEN c.user1_id = $1 THEN c.user1_deleted ELSE c.user2_deleted END = false
       ORDER BY c.last_message_at DESC NULLS LAST`,
      [req.user.userId]
    );

    // Get other user details
    const result = [];
    for (const chat of chats) {
      const otherUser = await queryOne("SELECT id, name, avatar, photo_url, gender, verified FROM users WHERE id = $1", [chat.other_user_id]);
      result.push({
        id: chat.id,
        otherUser: otherUser ? {
          id: otherUser.id,
          name: otherUser.name,
          avatar: otherUser.avatar,
          photoUrl: otherUser.photo_url,
          gender: otherUser.gender,
          verified: otherUser.verified,
        } : null,
        lastMessage: chat.last_message,
        lastMessageAt: chat.last_message_at,
        unreadCount: parseInt(chat.unread_count || "0"),
      });
    }

    return res.json({ ok: true, chats: result });
  } catch (err) {
    console.error("Get chats error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /chats/:id/messages — Get messages for a chat
// ============================================
router.get("/:id/messages", async (req, res) => {
  try {
    const { limit = 50, before } = req.query;

    // Verify user is part of chat
    const chat = await queryOne(
      "SELECT * FROM chats WHERE id = $1 AND (user1_id = $2 OR user2_id = $2)",
      [req.params.id, req.user.userId]
    );
    if (!chat) return res.status(404).json({ error: "Chat not found" });

    let sql = `
      SELECT m.*, u.name as sender_name, u.avatar as sender_avatar
      FROM messages m
      JOIN users u ON u.id = m.sender_id
      WHERE m.chat_id = $1
    `;
    const params = [req.params.id];

    if (before) {
      sql += ` AND m.created_at < $2`;
      params.push(before);
    }

    sql += ` ORDER BY m.created_at DESC LIMIT $${params.length + 1}`;
    params.push(parseInt(limit));

    const messages = await queryMany(sql, params);

    return res.json({
      ok: true,
      messages: messages.reverse().map((m) => ({
        id: m.id,
        chatId: m.chat_id,
        senderId: m.sender_id,
        senderName: m.sender_name,
        senderAvatar: m.sender_avatar,
        text: m.text,
        read: m.read,
        createdAt: m.created_at,
        isMe: m.sender_id === req.user.userId,
      })),
    });
  } catch (err) {
    console.error("Get messages error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /chats/:targetUserId/messages — Send message (creates chat if needed)
// ============================================
router.post("/:targetUserId/messages", async (req, res) => {
  try {
    const { text } = req.body;
    if (!text || !text.trim()) {
      return res.status(400).json({ error: "Message text is required" });
    }

    const targetId = req.params.targetUserId;
    if (targetId === req.user.userId) {
      return res.status(400).json({ error: "Cannot message yourself" });
    }

    // Check if blocked
    const blocked = await queryOne(
      `SELECT 1 FROM blocked_users 
       WHERE (blocker_id = $1 AND blocked_id = $2) OR (blocker_id = $2 AND blocked_id = $1)`,
      [req.user.userId, targetId]
    );
    if (blocked) return res.status(403).json({ error: "Cannot message blocked user" });

    // Find or create chat (ensure consistent ordering)
    const [u1, u2] = [req.user.userId, targetId].sort();
    let chat = await queryOne(
      "SELECT * FROM chats WHERE user1_id = $1 AND user2_id = $2",
      [u1, u2]
    );

    if (!chat) {
      chat = await queryOne(
        `INSERT INTO chats (user1_id, user2_id) VALUES ($1, $2) RETURNING *`,
        [u1, u2]
      );
    }

    // Reset deleted flags if re-opening chat
    if ((chat.user1_id === req.user.userId && chat.user1_deleted) ||
        (chat.user2_id === req.user.userId && chat.user2_deleted)) {
      const field = chat.user1_id === req.user.userId ? "user1_deleted" : "user2_deleted";
      await query(`UPDATE chats SET ${field} = false WHERE id = $1`, [chat.id]);
    }

    // Insert message
    const message = await queryOne(
      `INSERT INTO messages (chat_id, sender_id, text) VALUES ($1, $2, $3) RETURNING *`,
      [chat.id, req.user.userId, text.trim()]
    );

    // Update last_message_at
    await query("UPDATE chats SET last_message_at = NOW() WHERE id = $1", [chat.id]);

    return res.json({
      ok: true,
      chatId: chat.id,
      message: {
        id: message.id,
        chatId: message.chat_id,
        senderId: message.sender_id,
        text: message.text,
        read: message.read,
        createdAt: message.created_at,
        isMe: true,
      },
    });
  } catch (err) {
    console.error("Send message error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// PUT /chats/:id/read — Mark all messages as read
// ============================================
router.put("/:id/read", async (req, res) => {
  try {
    await query(
      "UPDATE messages SET read = true WHERE chat_id = $1 AND sender_id != $2 AND read = false",
      [req.params.id, req.user.userId]
    );
    return res.json({ ok: true });
  } catch (err) {
    console.error("Mark read error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// DELETE /chats/:id — Soft-delete chat
// ============================================
router.delete("/:id", async (req, res) => {
  try {
    const chat = await queryOne(
      "SELECT * FROM chats WHERE id = $1 AND (user1_id = $2 OR user2_id = $2)",
      [req.params.id, req.user.userId]
    );
    if (!chat) return res.status(404).json({ error: "Chat not found" });

    const field = chat.user1_id === req.user.userId ? "user1_deleted" : "user2_deleted";
    await query(`UPDATE chats SET ${field} = true WHERE id = $1`, [req.params.id]);

    // If both deleted, permanently delete
    const updated = await queryOne("SELECT * FROM chats WHERE id = $1", [req.params.id]);
    if (updated.user1_deleted && updated.user2_deleted) {
      await query("DELETE FROM chats WHERE id = $1", [req.params.id]); // CASCADE deletes messages
      console.log(`🗑️ Chat ${req.params.id} permanently deleted (both users deleted)`);
    }

    return res.json({ ok: true });
  } catch (err) {
    console.error("Delete chat error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /chats/unread-count — Total unread messages
// ============================================
router.get("/unread-count", async (req, res) => {
  try {
    const result = await queryOne(
      `SELECT COUNT(*) as count FROM messages m
       JOIN chats c ON c.id = m.chat_id
       WHERE (c.user1_id = $1 OR c.user2_id = $1)
         AND m.sender_id != $1
         AND m.read = false`,
      [req.user.userId]
    );
    return res.json({ ok: true, count: parseInt(result.count) });
  } catch (err) {
    console.error("Unread count error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

module.exports = router;
