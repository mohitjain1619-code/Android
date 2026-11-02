const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");
const NotificationService = require("../services/notificationService");

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
              (SELECT COUNT(*) FROM messages m WHERE m.chat_id = c.id AND m.sender_id != $1 AND m.read = false AND NOT ($1 = ANY(COALESCE(m.deleted_for, '{}')))) as unread_count,
              (SELECT m.text FROM messages m WHERE m.chat_id = c.id AND NOT ($1 = ANY(COALESCE(m.deleted_for, '{}'))) ORDER BY m.created_at DESC LIMIT 1) as last_message,
              (SELECT m.created_at FROM messages m WHERE m.chat_id = c.id AND NOT ($1 = ANY(COALESCE(m.deleted_for, '{}'))) ORDER BY m.created_at DESC LIMIT 1) as last_message_at
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

    // Verify user is part of chat. The parameter can be either the chatId or the other user's ID!
    let chat = await queryOne(
      "SELECT * FROM chats WHERE id = $1 AND (user1_id = $2 OR user2_id = $2)",
      [req.params.id, req.user.userId]
    );

    if (!chat) {
      chat = await queryOne(
        "SELECT * FROM chats WHERE ((user1_id = $1 AND user2_id = $2) OR (user1_id = $2 AND user2_id = $1))",
        [req.user.userId, req.params.id]
      );
    }

    if (!chat) return res.status(404).json({ error: "Chat not found" });

    const resolvedChatId = chat.id;
    let sql = `
      SELECT m.*, u.name as sender_name, u.avatar as sender_avatar
      FROM messages m
      JOIN users u ON u.id = m.sender_id
      WHERE m.chat_id = $1 AND NOT ($2 = ANY(COALESCE(m.deleted_for, '{}')))
    `;
    const params = [resolvedChatId, req.user.userId];

    if (before) {
      sql += ` AND m.created_at < $3`;
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

    // Emit real-time socket notification to the recipient
    try {
      const io = req.app.get("io");
      const redisModule = req.app.get("redisModule");
      if (io && redisModule) {
        const targetSocket = await redisModule.getUserSocket(targetId);
        if (targetSocket) {
          io.to(targetSocket).emit("new_message", {
            senderId: req.user.userId,
            chatId: chat.id,
            text: text.trim(),
            messageId: message.id,
            createdAt: message.created_at,
          });
        }
        // Also emit to sender for multi-device sync
        const senderSocket = await redisModule.getUserSocket(req.user.userId);
        if (senderSocket) {
          io.to(senderSocket).emit("new_message", {
            senderId: req.user.userId,
            chatId: chat.id,
            text: text.trim(),
            messageId: message.id,
            createdAt: message.created_at,
          });
        }
      }
    } catch (socketErr) {
      console.error("Socket emit error (non-fatal):", socketErr);
    }

    // Trigger high-priority push notification to recipient
    (async () => {
      try {
        const sender = await queryOne("SELECT name, avatar FROM users WHERE id = $1", [req.user.userId]);
        NotificationService.sendToUser(targetId, {
          title: sender ? sender.name : "New Message",
          body: text.trim(),
          type: "chat",
          channelId: "camverz_chats",
          highPriority: true,
          data: {
            targetUserId: req.user.userId,
            targetUserName: sender ? sender.name : "User",
            targetUserAvatar: sender ? (sender.avatar || "") : "",
            chatId: chat.id,
            messageId: message.id,
          },
        });
      } catch (fcmErr) {
        console.error("Error triggering chat push notification:", fcmErr.message);
      }
    })();

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
// DELETE /chats/messages/:messageId — Delete single message
// Query / Body: ?forEveryone=true|false
// ============================================
router.delete("/messages/:messageId", async (req, res) => {
  try {
    const { messageId } = req.params;
    const forEveryone = req.query.forEveryone === "true" || req.body?.forEveryone === true;

    // Retrieve message along with chat participants
    const message = await queryOne(
      `SELECT m.*, c.user1_id, c.user2_id 
       FROM messages m 
       JOIN chats c ON c.id = m.chat_id 
       WHERE m.id = $1`,
      [messageId]
    );

    if (!message) {
      return res.status(404).json({ error: "Message not found" });
    }

    // Verify user is a participant of the chat
    if (message.user1_id !== req.user.userId && message.user2_id !== req.user.userId) {
      return res.status(403).json({ error: "Unauthorized to access this message" });
    }

    const otherUserId = message.user1_id === req.user.userId ? message.user2_id : message.user1_id;

    if (forEveryone) {
      // "Delete for everyone": Only the sender can unsend
      if (message.sender_id !== req.user.userId) {
        return res.status(403).json({ error: "You can only delete your own messages for everyone" });
      }

      await query("DELETE FROM messages WHERE id = $1", [messageId]);

      // Emit real-time socket event to both users
      try {
        const io = req.app.get("io");
        const redisModule = req.app.get("redisModule");
        if (io && redisModule) {
          const payload = {
            messageId: message.id,
            chatId: message.chat_id,
            forEveryone: true,
          };
          const targetSocket = await redisModule.getUserSocket(otherUserId);
          if (targetSocket) {
            io.to(targetSocket).emit("message_deleted", payload);
          }
          const mySocket = await redisModule.getUserSocket(req.user.userId);
          if (mySocket) {
            io.to(mySocket).emit("message_deleted", payload);
          }
        }
      } catch (sockErr) {
        console.error("Socket emit error for message_deleted:", sockErr);
      }

      return res.json({ ok: true, messageId: message.id, forEveryone: true });
    } else {
      // "Delete for me": Soft-delete for the requesting user only
      await query(
        `UPDATE messages 
         SET deleted_for = array_append(COALESCE(deleted_for, '{}'), $1::uuid) 
         WHERE id = $2 AND NOT ($1::uuid = ANY(COALESCE(deleted_for, '{}')))`,
        [req.user.userId, messageId]
      );

      // If both participants deleted the message for themselves, permanently purge it
      await query(
        `DELETE FROM messages 
         WHERE id = $1 
           AND $2::uuid = ANY(COALESCE(deleted_for, '{}')) 
           AND $3::uuid = ANY(COALESCE(deleted_for, '{}'))`,
        [messageId, message.user1_id, message.user2_id]
      );

      return res.json({ ok: true, messageId: message.id, forEveryone: false });
    }
  } catch (err) {
    console.error("Delete message error:", err);
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
