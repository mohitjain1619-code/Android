const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();
router.use(requireAuth);

// ============================================
// GET /posts — Feed (with category filter + expiry check)
// ============================================
router.get("/", async (req, res) => {
  try {
    const { category, userId, limit = 50, offset = 0 } = req.query;
    let sql = `
      SELECT p.*, u.name as username, u.avatar as user_avatar, u.photo_url as user_photo_url,
             u.verified as verified, u.gender as gender, u.sex_preference as sex_preference,
             (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) as like_count,
             (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) as comment_count,
             EXISTS(SELECT 1 FROM post_likes pl WHERE pl.post_id = p.id AND pl.user_id = $1) as liked_by_me
      FROM posts p
      JOIN users u ON u.id = p.user_id
      WHERE p.expiry_at > NOW()
    `;
    const params = [req.user.userId];
    let paramIdx = 2;

    // Exclude posts from blocked users
    sql += ` AND p.user_id NOT IN (SELECT blocked_id FROM blocked_users WHERE blocker_id = $1)`;
    sql += ` AND p.user_id NOT IN (SELECT blocker_id FROM blocked_users WHERE blocked_id = $1)`;

    if (category && category !== "all") {
      sql += ` AND p.category = $${paramIdx}`;
      params.push(category);
      paramIdx++;
    }

    if (userId) {
      sql += ` AND p.user_id = $${paramIdx}`;
      params.push(userId);
      paramIdx++;
    }

    sql += ` ORDER BY p.created_at DESC LIMIT $${paramIdx} OFFSET $${paramIdx + 1}`;
    params.push(parseInt(limit), parseInt(offset));

    const posts = await queryMany(sql, params);

    return res.json({
      ok: true,
      posts: posts.map(formatPost),
    });
  } catch (err) {
    console.error("Get posts error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /posts — Create a text post
// ============================================
router.post("/", async (req, res) => {
  try {
    const { text, category } = req.body;

    if (!text || !text.trim()) {
      return res.status(400).json({ error: "Post text is required" });
    }
    if (!category) {
      return res.status(400).json({ error: "Category is required" });
    }

    // 2 hours expiry
    const expiryAt = new Date(Date.now() + 2 * 60 * 60 * 1000);

    const post = await queryOne(
      `INSERT INTO posts (user_id, text, category, expiry_at)
       VALUES ($1, $2, $3, $4) RETURNING *`,
      [req.user.userId, text.trim(), category.toLowerCase(), expiryAt]
    );

    // Get user info for response
    const user = await queryOne("SELECT name, avatar, photo_url, verified, gender, sex_preference FROM users WHERE id = $1", [req.user.userId]);

    return res.json({
      ok: true,
      post: {
        ...formatPost({ ...post, username: user.name, user_avatar: user.avatar, user_photo_url: user.photo_url, verified: user.verified, gender: user.gender, sex_preference: user.sex_preference, like_count: "0", comment_count: "0", liked_by_me: false }),
      },
    });
  } catch (err) {
    console.error("Create post error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /posts/:id — Get single post
// ============================================
router.get("/:id", async (req, res) => {
  try {
    const post = await queryOne(
      `SELECT p.*, u.name as username, u.avatar as user_avatar, u.photo_url as user_photo_url,
              u.verified as verified, u.gender as gender, u.sex_preference as sex_preference,
              (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) as like_count,
              (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) as comment_count,
              EXISTS(SELECT 1 FROM post_likes pl WHERE pl.post_id = p.id AND pl.user_id = $2) as liked_by_me
       FROM posts p
       JOIN users u ON u.id = p.user_id
       WHERE p.id = $1`,
      [req.params.id, req.user.userId]
    );

    if (!post) return res.status(404).json({ error: "Post not found" });
    return res.json({ ok: true, post: formatPost(post) });
  } catch (err) {
    console.error("Get post error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// DELETE /posts/:id — Delete a post (owner only)
// ============================================
router.delete("/:id", async (req, res) => {
  try {
    const post = await queryOne("SELECT user_id FROM posts WHERE id = $1", [req.params.id]);
    if (!post) return res.status(404).json({ error: "Post not found" });
    if (post.user_id !== req.user.userId) {
      return res.status(403).json({ error: "Not your post" });
    }

    // CASCADE handles likes, comments, notifications
    await query("DELETE FROM posts WHERE id = $1", [req.params.id]);
    return res.json({ ok: true });
  } catch (err) {
    console.error("Delete post error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /posts/:id/like — Toggle like
// ============================================
router.post("/:id/like", async (req, res) => {
  try {
    const existing = await queryOne(
      "SELECT 1 FROM post_likes WHERE post_id = $1 AND user_id = $2",
      [req.params.id, req.user.userId]
    );

    if (existing) {
      await query("DELETE FROM post_likes WHERE post_id = $1 AND user_id = $2", [
        req.params.id,
        req.user.userId,
      ]);
    } else {
      await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [
        req.params.id,
        req.user.userId,
      ]);

      // Notification for post owner
      const post = await queryOne("SELECT user_id FROM posts WHERE id = $1", [req.params.id]);
      if (post && post.user_id !== req.user.userId) {
        await query(
          `INSERT INTO notifications (user_id, type, triggering_user_id, post_id)
           VALUES ($1, 'like', $2, $3)`,
          [post.user_id, req.user.userId, req.params.id]
        );
      }
    }

    const count = await queryOne("SELECT COUNT(*) as count FROM post_likes WHERE post_id = $1", [req.params.id]);

    return res.json({ ok: true, liked: !existing, likeCount: parseInt(count.count) });
  } catch (err) {
    console.error("Like error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// GET /posts/:id/comments — Get comments for a post
// ============================================
router.get("/:id/comments", async (req, res) => {
  try {
    const comments = await queryMany(
      `SELECT c.*, u.name as username, u.avatar as user_avatar, u.photo_url as user_photo_url,
              (SELECT COUNT(*) FROM comment_likes cl WHERE cl.comment_id = c.id) as like_count,
              EXISTS(SELECT 1 FROM comment_likes cl WHERE cl.comment_id = c.id AND cl.user_id = $2) as liked_by_me
       FROM comments c
       JOIN users u ON u.id = c.user_id
       WHERE c.post_id = $1
       ORDER BY c.created_at ASC`,
      [req.params.id, req.user.userId]
    );

    return res.json({
      ok: true,
      comments: comments.map((c) => ({
        id: c.id,
        postId: c.post_id,
        userId: c.user_id,
        parentId: c.parent_id,
        username: c.username,
        userAvatar: c.user_avatar,
        userPhotoUrl: c.user_photo_url,
        text: c.text,
        likeCount: parseInt(c.like_count || "0"),
        likedByMe: c.liked_by_me || false,
        createdAt: c.created_at,
      })),
    });
  } catch (err) {
    console.error("Get comments error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /posts/:id/comments — Add a comment
// ============================================
router.post("/:id/comments", async (req, res) => {
  try {
    const { text, parentId } = req.body;
    if (!text || !text.trim()) {
      return res.status(400).json({ error: "Comment text is required" });
    }

    const comment = await queryOne(
      `INSERT INTO comments (post_id, user_id, text, parent_id) VALUES ($1, $2, $3, $4) RETURNING *`,
      [req.params.id, req.user.userId, text.trim(), parentId || null]
    );

    // Notification for post owner
    const post = await queryOne("SELECT user_id FROM posts WHERE id = $1", [req.params.id]);
    if (post && post.user_id !== req.user.userId) {
      await query(
        `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
         VALUES ($1, 'comment', $2, $3, $4)`,
        [post.user_id, req.user.userId, req.params.id, comment.id]
      );
    }

    // Notification for parent comment owner (if reply)
    if (parentId) {
      const parentComment = await queryOne("SELECT user_id FROM comments WHERE id = $1", [parentId]);
      if (parentComment && parentComment.user_id !== req.user.userId) {
        await query(
          `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
           VALUES ($1, 'reply', $2, $3, $4)`,
          [parentComment.user_id, req.user.userId, req.params.id, comment.id]
        );
      }
    }

    const user = await queryOne("SELECT name, avatar, photo_url FROM users WHERE id = $1", [req.user.userId]);

    return res.json({
      ok: true,
      comment: {
        id: comment.id,
        postId: comment.post_id,
        userId: comment.user_id,
        parentId: comment.parent_id,
        username: user.name,
        userAvatar: user.avatar,
        userPhotoUrl: user.photo_url,
        text: comment.text,
        likeCount: 0,
        likedByMe: false,
        createdAt: comment.created_at,
      },
    });
  } catch (err) {
    console.error("Add comment error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// POST /posts/comments/:id/like — Toggle like on a comment
// ============================================
router.post("/comments/:id/like", async (req, res) => {
  try {
    const commentId = req.params.id;
    const userId = req.user.userId;

    const existing = await queryOne(
      "SELECT * FROM comment_likes WHERE comment_id = $1 AND user_id = $2",
      [commentId, userId]
    );

    if (existing) {
      await query("DELETE FROM comment_likes WHERE comment_id = $1 AND user_id = $2", [
        commentId,
        userId,
      ]);
    } else {
      await query("INSERT INTO comment_likes (comment_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [
        commentId,
        userId,
      ]);

      // Notify comment owner of the like
      const commentObj = await queryOne("SELECT user_id, post_id FROM comments WHERE id = $1", [commentId]);
      if (commentObj && commentObj.user_id !== userId) {
        await query(
          `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
           VALUES ($1, 'like', $2, $3, $4)`,
          [commentObj.user_id, userId, commentObj.post_id, commentId]
        );
      }
    }

    const count = await queryOne("SELECT COUNT(*) as count FROM comment_likes WHERE comment_id = $1", [commentId]);

    return res.json({
      ok: true,
      liked: !existing,
      likeCount: parseInt(count.count),
    });
  } catch (err) {
    console.error("Comment like error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// ============================================
// DELETE /posts/:postId/comments/:commentId — Delete a comment
// ============================================
router.delete("/:postId/comments/:commentId", async (req, res) => {
  try {
    const comment = await queryOne("SELECT user_id FROM comments WHERE id = $1", [req.params.commentId]);
    if (!comment) return res.status(404).json({ error: "Comment not found" });
    if (comment.user_id !== req.user.userId) {
      return res.status(403).json({ error: "Not your comment" });
    }

    await query("DELETE FROM comments WHERE id = $1", [req.params.commentId]);
    return res.json({ ok: true });
  } catch (err) {
    console.error("Delete comment error:", err);
    return res.status(500).json({ error: "Internal error" });
  }
});

// Helper: format post for API response
function formatPost(p) {
  return {
    id: p.id,
    userId: p.user_id,
    text: p.text,
    category: p.category,
    type: "text",
    username: p.username,
    userAvatar: p.user_avatar,
    userPhotoUrl: p.user_photo_url,
    verified: p.verified || false,
    gender: p.gender || "male",
    sexPreference: p.sex_preference || "Straight",
    likeCount: parseInt(p.like_count || "0"),
    commentCount: parseInt(p.comment_count || "0"),
    likedByMe: p.liked_by_me || false,
    expiryAt: p.expiry_at,
    createdAt: p.created_at,
  };
}

module.exports = router;
