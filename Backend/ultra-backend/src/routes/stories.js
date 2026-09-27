const express = require("express");
const multer = require("multer");
const fs = require("fs");
const path = require("path");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();

// Ensure uploads directory exists
const storiesDir = path.join(__dirname, "../../uploads/stories");
if (!fs.existsSync(storiesDir)) {
  fs.mkdirSync(storiesDir, { recursive: true });
}

// Multer storage configuration for stories uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, storiesDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname);
    cb(null, `story-${uniqueSuffix}${ext}`);
  }
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 15 * 1024 * 1024 } // Max 15MB file size
});

router.use(requireAuth);

// Admin Email Constraint constant
const ADMIN_EMAIL = "mohitjain1619@gmail.com";

// ============================================
// GET /api/stories/active — Fetch Active Stories grouped by user
// ============================================
router.get("/active", async (req, res) => {
  try {
    const currentUserId = req.user ? req.user.userId : null;
    const rows = await queryMany(
      `SELECT s.*, u.name as user_name, u.avatar as user_avatar, u.photo_url as user_photo_url, u.email as user_email,
              (SELECT COUNT(*)::int FROM story_views v WHERE v.story_id = s.id) as views_count,
              EXISTS(SELECT 1 FROM story_views v WHERE v.story_id = s.id AND v.viewer_id = $1) as has_viewed
       FROM stories s
       JOIN users u ON u.id = s.user_id
       WHERE s.expires_at > NOW()
       ORDER BY s.created_at ASC`,
      [currentUserId]
    );

    const grouped = {};
    rows.forEach(r => {
      const uId = r.user_id;
      if (!grouped[uId]) {
        grouped[uId] = {
          userId: uId,
          userName: r.user_name,
          userAvatar: r.user_avatar,
          userPhotoUrl: r.user_photo_url,
          userEmail: r.user_email,
          stories: []
        };
      }
      grouped[uId].stories.push({
        id: r.id,
        userId: r.user_id,
        type: r.type,
        mediaUrl: r.media_url,
        textContent: r.text_content,
        textColor: r.text_color,
        bgGradient: r.bg_gradient,
        viewsCount: r.views_count || 0,
        hasViewed: !!r.has_viewed,
        createdAt: new Date(r.created_at).getTime(),
        expiresAt: new Date(r.expires_at).getTime()
      });
    });

    res.json({ ok: true, usersWithStories: Object.values(grouped) });
  } catch (err) {
    console.error("Get active stories error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// POST /api/stories/:id/view — Record story view
// ============================================
router.post("/:id/view", async (req, res) => {
  try {
    const storyId = req.params.id;
    const viewerId = req.user.userId;

    if (!storyId || !viewerId) {
      return res.status(400).json({ ok: false, error: "Missing storyId or viewerId" });
    }

    await query(
      `INSERT INTO story_views (story_id, viewer_id)
       VALUES ($1, $2)
       ON CONFLICT (story_id, viewer_id) DO NOTHING`,
      [storyId, viewerId]
    );

    res.json({ ok: true, message: "Story view recorded" });
  } catch (err) {
    console.error("Record story view error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// GET /api/stories/:id/viewers — Get story viewers list
// ============================================
router.get("/:id/viewers", async (req, res) => {
  try {
    const storyId = req.params.id;
    const story = await queryOne("SELECT * FROM stories WHERE id = $1", [storyId]);
    if (!story) {
      return res.status(404).json({ ok: false, error: "Story not found" });
    }

    if (story.user_id !== req.user.userId && req.user.email !== ADMIN_EMAIL) {
      return res.status(403).json({ ok: false, error: "Access denied" });
    }

    const viewers = await queryMany(
      `SELECT v.created_at as viewed_at, u.id, u.name, u.avatar, u.photo_url
       FROM story_views v
       JOIN users u ON u.id = v.viewer_id
       WHERE v.story_id = $1
       ORDER BY v.created_at DESC`,
      [storyId]
    );

    res.json({ ok: true, viewCount: viewers.length, viewers });
  } catch (err) {
    console.error("Get story viewers error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// POST /api/stories/upload — Upload/Publish Story (Admin-Only)
// ============================================
router.post("/upload", upload.single("media"), async (req, res) => {
  try {
    if (req.user.email !== ADMIN_EMAIL) {
      return res.status(403).json({ ok: false, error: "Access denied. Admin only." });
    }

    const { type, textContent, textColor, bgGradient } = req.body;
    if (!type) {
      return res.status(400).json({ ok: false, error: "Missing story type" });
    }

    let mediaUrl = null;
    if (type === "IMAGE" || type === "VIDEO") {
      if (!req.file) {
        return res.status(400).json({ ok: false, error: "Missing media file attachment" });
      }
      mediaUrl = `/uploads/stories/${req.file.filename}`;
    }

    const inserted = await queryOne(
      `INSERT INTO stories (user_id, type, media_url, text_content, text_color, bg_gradient)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [req.user.userId, type, mediaUrl, textContent || "", textColor || "#FFFFFF", bgGradient || ""]
    );

    const io = req.app.get("io");
    if (io) {
      io.emit("realmeet-stories-updated");
    }

    res.json({ ok: true, story: inserted });
  } catch (err) {
    console.error("Upload story error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// DELETE /api/stories/:id — Delete Story (Admin-Only)
// ============================================
router.delete("/:id", async (req, res) => {
  try {
    if (req.user.email !== ADMIN_EMAIL) {
      return res.status(403).json({ ok: false, error: "Access denied. Admin only." });
    }

    const story = await queryOne("SELECT * FROM stories WHERE id = $1", [req.params.id]);
    if (!story) {
      return res.status(404).json({ ok: false, error: "Story not found" });
    }

    if (story.media_url) {
      const filename = path.basename(story.media_url);
      const filePath = path.join(storiesDir, filename);
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    }

    await query("DELETE FROM stories WHERE id = $1", [req.params.id]);

    const io = req.app.get("io");
    if (io) {
      io.emit("realmeet-stories-updated");
    }

    res.json({ ok: true, message: "Story deleted successfully" });
  } catch (err) {
    console.error("Delete story error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

module.exports = router;
