const express = require("express");
const { queryOne, queryMany, query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();
router.use(requireAuth);

// Helper to format post from DB representation to Client representation
function formatPost(r) {
  return {
    id: r.id,
    userId: r.user_id,
    type: r.type,
    hostUserId: r.user_id, // For Party parity
    userName: r.user_name,
    hostName: r.user_name,
    userAvatar: r.user_avatar,
    hostAvatar: r.user_avatar,
    photoUrl: r.photo_url,
    hostPhotoUrl: r.photo_url,
    age: r.age,
    hostAge: r.age,
    city: r.city,
    purpose: r.purpose,
    location: r.location,
    venue: r.location, // For Party parity
    time: r.meeting_time,
    partyTime: r.meeting_time, // For Party parity
    description: r.description,
    capacity: r.capacity,
    targetGender: r.target_gender,
    relationshipStatus: r.relationship_status,
    interests: r.interests,
    listVisibility: r.list_visibility,
    gender: r.gender,
    verified: r.verified,
    premium: r.premium,
    sexPreference: r.sex_preference,
    createdAt: new Date(r.created_at).getTime()
  };
}

// Helper to format request
function formatRequest(r) {
  return {
    id: r.id,
    postId: r.post_id,
    postTitle: r.post_title,
    posterUserId: r.poster_user_id,
    applicantUserId: r.applicant_user_id,
    applicantName: r.applicant_name,
    applicantAvatar: r.applicant_avatar,
    applicantPhotoUrl: r.applicant_photo_url,
    applicantAge: r.applicant_age,
    applicantCity: r.applicant_city,
    message: r.message,
    contactPreference: r.contact_preference,
    status: r.status,
    applicantGender: r.applicant_gender,
    applicantVerified: r.applicant_verified,
    createdAt: new Date(r.created_at).getTime()
  };
}

// ============================================
// GET /api/realmeet/feed — Fetch Community Feed
// ============================================
router.get("/feed", async (req, res) => {
  try {
    const rows = await queryMany(
      `SELECT cp.*, u.name as user_name, u.avatar as user_avatar, u.photo_url, u.gender, u.verified, u.is_premium as premium, u.sex_preference, u.dob
       FROM community_posts cp
       JOIN users u ON u.id = cp.user_id
       ORDER BY cp.created_at DESC`
    );

    const posts = rows.map(r => {
      // Calculate age from dob
      let age = 22;
      if (r.dob) {
        try {
          const parts = r.dob.split('/');
          if (parts.length === 3) {
            const birthDate = new Date(parts[2], parts[1] - 1, parts[0]);
            const today = new Date();
            age = today.getFullYear() - birthDate.getFullYear();
            const m = today.getMonth() - birthDate.getMonth();
            if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
          }
        } catch (e) {}
      }
      return formatPost({ ...r, age });
    });

    res.json({
      ok: true,
      realMeetPosts: posts.filter(p => p.type === "REAL_MEET"),
      partyPosts: posts.filter(p => p.type === "PARTY"),
      fantasyPosts: posts.filter(p => p.type === "FANTASY")
    });
  } catch (err) {
    console.error("Get community feed error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// POST /api/realmeet/post — Publish Post
// ============================================
router.post("/post", async (req, res) => {
  try {
    const { type, post } = req.body;
    if (!type || !post) return res.status(400).json({ ok: false, error: "Missing type or post payload" });

    const postId = post.id || post.uuid;
    const purpose = post.purpose || "";
    const location = post.location || post.venue || "";
    const meetingTime = post.time || post.partyTime || "";
    const description = post.description || "";
    const capacity = post.capacity || 0;
    const targetGender = post.targetGender || "";
    const relationshipStatus = post.relationshipStatus || "";
    const interests = post.interests || "";
    const listVisibility = post.listVisibility || "PRIVATE";

    const inserted = await queryOne(
      `INSERT INTO community_posts (id, user_id, type, purpose, location, meeting_time, description, capacity, target_gender, relationship_status, interests, list_visibility)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
       ON CONFLICT (id) DO UPDATE SET
         purpose = EXCLUDED.purpose,
         location = EXCLUDED.location,
         meeting_time = EXCLUDED.meeting_time,
         description = EXCLUDED.description,
         capacity = EXCLUDED.capacity,
         target_gender = EXCLUDED.target_gender,
         relationship_status = EXCLUDED.relationship_status,
         interests = EXCLUDED.interests,
         list_visibility = EXCLUDED.list_visibility
       RETURNING *`,
      [postId, req.user.userId, type, purpose, location, meetingTime, description, capacity, targetGender, relationshipStatus, interests, listVisibility]
    );

    // Fetch poster details to broadcast
    const u = await queryOne("SELECT name as user_name, avatar as user_avatar, photo_url, gender, verified, is_premium as premium, sex_preference FROM users WHERE id = $1", [req.user.userId]);

    const formatted = formatPost({ ...inserted, ...u });

    const io = req.app.get("io");
    if (io) {
      io.emit("realmeet-post-updated", { type, post: formatted });
    }

    res.json({ ok: true, post: formatted });
  } catch (err) {
    console.error("Publish community post error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// DELETE /api/realmeet/post/:id — Delete Post
// ============================================
router.delete("/post/:id", async (req, res) => {
  try {
    const post = await queryOne("SELECT * FROM community_posts WHERE id = $1", [req.params.id]);
    if (!post) return res.status(404).json({ ok: false, error: "Post not found" });

    if (post.user_id !== req.user.userId) {
      return res.status(403).json({ ok: false, error: "Not authorized to delete this post" });
    }

    await query("DELETE FROM community_posts WHERE id = $1", [req.params.id]);

    const io = req.app.get("io");
    if (io) {
      io.emit("realmeet-post-deleted", { postId: req.params.id });
    }

    res.json({ ok: true, message: "Post deleted successfully" });
  } catch (err) {
    console.error("Delete community post error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// POST /api/realmeet/post/:id/save — Save Party
// ============================================
router.post("/post/:id/save", async (req, res) => {
  try {
    await query(
      "INSERT INTO saved_parties (user_id, post_id) VALUES ($1, $2) ON CONFLICT DO NOTHING",
      [req.user.userId, req.params.id]
    );
    res.json({ ok: true, message: "Party saved successfully" });
  } catch (err) {
    console.error("Save party error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// DELETE /api/realmeet/post/:id/save — Unsave Party
// ============================================
router.delete("/post/:id/save", async (req, res) => {
  try {
    await query(
      "DELETE FROM saved_parties WHERE user_id = $1 AND post_id = $2",
      [req.user.userId, req.params.id]
    );
    res.json({ ok: true, message: "Party unsaved successfully" });
  } catch (err) {
    console.error("Unsave party error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// GET /api/realmeet/saved-parties — Get Saved Parties List
// ============================================
router.get("/saved-parties", async (req, res) => {
  try {
    const rows = await queryMany(
      `SELECT cp.*, u.name as user_name, u.avatar as user_avatar, u.photo_url, u.gender, u.verified, u.is_premium as premium, u.sex_preference, u.dob
       FROM saved_parties sp
       JOIN community_posts cp ON cp.id = sp.post_id
       JOIN users u ON u.id = cp.user_id
       WHERE sp.user_id = $1
       ORDER BY sp.created_at DESC`,
      [req.user.userId]
    );

    const posts = rows.map(r => {
      let age = 22;
      if (r.dob) {
        try {
          const parts = r.dob.split('/');
          if (parts.length === 3) {
            const birthDate = new Date(parts[2], parts[1] - 1, parts[0]);
            const today = new Date();
            age = today.getFullYear() - birthDate.getFullYear();
            const m = today.getMonth() - birthDate.getMonth();
            if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
          }
        } catch (e) {}
      }
      return formatPost({ ...r, age });
    });

    res.json({ ok: true, savedParties: posts });
  } catch (err) {
    console.error("Get saved parties error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// GET /api/realmeet/requests — Fetch Requests (Host/User View)
// ============================================
router.get("/requests", async (req, res) => {
  try {
    const rows = await queryMany(
      `SELECT cr.*, cp.purpose as post_title,
              u.name as applicant_name, u.avatar as applicant_avatar, u.photo_url as applicant_photo_url, u.gender as applicant_gender, u.verified as applicant_verified, u.dob
       FROM community_requests cr
       JOIN community_posts cp ON cp.id = cr.post_id
       JOIN users u ON u.id = cr.applicant_user_id
       WHERE cr.poster_user_id = $1 OR cr.applicant_user_id = $1
       ORDER BY cr.created_at DESC`,
      [req.user.userId]
    );

    const requests = rows.map(r => {
      let age = 22;
      if (r.dob) {
        try {
          const parts = r.dob.split('/');
          if (parts.length === 3) {
            const birthDate = new Date(parts[2], parts[1] - 1, parts[0]);
            const today = new Date();
            age = today.getFullYear() - birthDate.getFullYear();
            const m = today.getMonth() - birthDate.getMonth();
            if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
          }
        } catch (e) {}
      }
      return formatRequest({ ...r, applicant_age: age });
    });

    res.json({ ok: true, requests });
  } catch (err) {
    console.error("Get community requests error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// GET /api/realmeet/post/:id/members — Fetch Accepted Party Guests
// ============================================
router.get("/post/:id/members", async (req, res) => {
  try {
    const post = await queryOne("SELECT * FROM community_posts WHERE id = $1", [req.params.id]);
    if (!post) return res.status(404).json({ ok: false, error: "Post not found" });

    // Check visibility restrictions
    if (post.list_visibility === "PRIVATE") {
      const isHost = post.user_id === req.user.userId;
      const isAccepted = await queryOne(
        "SELECT 1 FROM community_requests WHERE post_id = $1 AND applicant_user_id = $2 AND status = 'ACCEPTED'",
        [req.params.id, req.user.userId]
      );
      if (!isHost && !isAccepted) {
        return res.status(403).json({ ok: false, error: "This guest list is private." });
      }
    }

    const rows = await queryMany(
      `SELECT cr.*, u.name as applicant_name, u.avatar as applicant_avatar, u.photo_url as applicant_photo_url, u.gender as applicant_gender, u.verified as applicant_verified
       FROM community_requests cr
       JOIN users u ON u.id = cr.applicant_user_id
       WHERE cr.post_id = $1 AND cr.status = 'ACCEPTED'
       ORDER BY cr.created_at ASC`,
      [req.params.id]
    );

    res.json({
      ok: true,
      members: rows.map(r => ({
        userId: r.applicant_user_id,
        name: r.applicant_name,
        avatar: r.applicant_avatar,
        photoUrl: r.applicant_photo_url,
        gender: r.applicant_gender,
        verified: r.applicant_verified
      }))
    });
  } catch (err) {
    console.error("Get party members error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// PUT /api/realmeet/post/:id/visibility — Set Visibility
// ============================================
router.put("/post/:id/visibility", async (req, res) => {
  try {
    const { visibility } = req.body;
    if (!visibility || (visibility !== "PUBLIC" && visibility !== "PRIVATE")) {
      return res.status(400).json({ ok: false, error: "Invalid visibility mode" });
    }

    const post = await queryOne("SELECT * FROM community_posts WHERE id = $1", [req.params.id]);
    if (!post) return res.status(404).json({ ok: false, error: "Post not found" });

    if (post.user_id !== req.user.userId) {
      return res.status(403).json({ ok: false, error: "Not authorized" });
    }

    await query("UPDATE community_posts SET list_visibility = $1 WHERE id = $2", [visibility, req.params.id]);
    res.json({ ok: true, visibility });
  } catch (err) {
    console.error("Set party visibility error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// POST /api/realmeet/post/:id/announcements — Post Announcement
// ============================================
router.post("/post/:id/announcements", async (req, res) => {
  try {
    const { text } = req.body;
    if (!text) return res.status(400).json({ ok: false, error: "Missing announcement text" });

    const post = await queryOne("SELECT * FROM community_posts WHERE id = $1", [req.params.id]);
    if (!post) return res.status(404).json({ ok: false, error: "Post not found" });

    if (post.user_id !== req.user.userId) {
      return res.status(403).json({ ok: false, error: "Only host can broadcast announcements" });
    }

    const announcement = await queryOne(
      "INSERT INTO party_announcements (party_id, host_id, text) VALUES ($1, $2, $3) RETURNING *",
      [req.params.id, req.user.userId, text]
    );

    // Get all accepted members to notify
    const members = await queryMany(
      "SELECT applicant_user_id FROM community_requests WHERE post_id = $1 AND status = 'ACCEPTED'",
      [req.params.id]
    );

    // Insert database notifications
    for (let m of members) {
      await query(
        "INSERT INTO notifications (user_id, type, triggering_user_id, community_post_id) VALUES ($1, 'party_announcement', $2, $3)",
        [m.applicant_user_id, req.user.userId, req.params.id]
      );
    }

    const io = req.app.get("io");
    if (io) {
      // Emit real-time announcements
      members.forEach(m => {
        io.emit(`party-announcement-received-${m.applicant_user_id}`, {
          partyId: req.params.id,
          text,
          createdAt: new Date(announcement.created_at).getTime()
        });
      });
    }

    res.json({ ok: true, announcement });
  } catch (err) {
    console.error("Post announcement error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// GET /api/realmeet/post/:id/announcements — Fetch Announcements
// ============================================
router.get("/post/:id/announcements", async (req, res) => {
  try {
    const post = await queryOne("SELECT * FROM community_posts WHERE id = $1", [req.params.id]);
    if (!post) return res.status(404).json({ ok: false, error: "Post not found" });

    const isHost = post.user_id === req.user.userId;
    const isAccepted = await queryOne(
      "SELECT 1 FROM community_requests WHERE post_id = $1 AND applicant_user_id = $2 AND status = 'ACCEPTED'",
      [req.params.id, req.user.userId]
    );

    if (!isHost && !isAccepted) {
      return res.status(403).json({ ok: false, error: "Access denied. Only listed guests can view announcements." });
    }

    const announcements = await queryMany(
      "SELECT * FROM party_announcements WHERE party_id = $1 ORDER BY created_at DESC",
      [req.params.id]
    );

    res.json({
      ok: true,
      announcements: announcements.map(a => ({
        id: a.id,
        text: a.text,
        createdAt: new Date(a.created_at).getTime()
      }))
    });
  } catch (err) {
    console.error("Get announcements error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// POST /api/realmeet/request — Send Join Request
// ============================================
router.post("/request", async (req, res) => {
  try {
    const { request } = req.body;
    if (!request) return res.status(400).json({ ok: false, error: "Missing request data" });

    const reqId = request.id || UUID.randomUUID().toString();
    const postId = request.postId;
    const postTitle = request.postTitle || "";
    const posterUserId = request.posterUserId;
    const message = request.message || "";
    const contactPref = request.contactPreference || "Private Video Call";

    // Insert request
    const inserted = await queryOne(
      `INSERT INTO community_requests (id, post_id, poster_user_id, applicant_user_id, message, contact_preference, status)
       VALUES ($1, $2, $3, $4, $5, $6, 'PENDING')
       ON CONFLICT (post_id, applicant_user_id) DO UPDATE SET
         message = EXCLUDED.message,
         contact_preference = EXCLUDED.contact_preference,
         status = 'PENDING'
       RETURNING *`,
      [reqId, postId, posterUserId, req.user.userId, message, contactPref]
    );

    // Create a notification for the host
    await query(
      "INSERT INTO notifications (user_id, type, triggering_user_id, community_post_id) VALUES ($1, 'realmeet_request', $2, $3)",
      [posterUserId, req.user.userId, postId]
    );

    // Get applicant user details to emit
    const u = await queryOne("SELECT name as user_name, avatar as user_avatar, photo_url, gender, verified, dob FROM users WHERE id = $1", [req.user.userId]);

    let age = 22;
    if (u.dob) {
      try {
        const parts = u.dob.split('/');
        const birthDate = new Date(parts[2], parts[1] - 1, parts[0]);
        const today = new Date();
        age = today.getFullYear() - birthDate.getFullYear();
      } catch (e) {}
    }

    const formatted = formatRequest({ ...inserted, ...u, applicant_age: age, post_title: postTitle });

    const io = req.app.get("io");
    if (io) {
      io.emit("realmeet-request-sent", formatted);
    }

    res.json({ ok: true, request: formatted });
  } catch (err) {
    console.error("Create request error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// PUT /api/realmeet/request/status — Accept/Reject Request
// ============================================
router.put("/request/status", async (req, res) => {
  try {
    const { requestId, status } = req.body;
    if (!requestId || !status) return res.status(400).json({ ok: false, error: "Missing requestId or status" });

    const request = await queryOne("SELECT * FROM community_requests WHERE id = $1", [requestId]);
    if (!request) return res.status(404).json({ ok: false, error: "Request not found" });

    // Verify authorized user
    if (request.poster_user_id !== req.user.userId) {
      return res.status(403).json({ ok: false, error: "Only the post owner can update request status" });
    }

    await query("UPDATE community_requests SET status = $1 WHERE id = $2", [status, requestId]);

    if (status === "ACCEPTED") {
      // Create accepted notification for applicant
      await query(
        "INSERT INTO notifications (user_id, type, triggering_user_id, community_post_id) VALUES ($1, 'realmeet_accepted', $2, $3)",
        [request.applicant_user_id, req.user.userId, request.post_id]
      );
    }

    const io = req.app.get("io");
    if (io) {
      io.emit("realmeet-status-updated", { requestId, status });
    }

    res.json({ ok: true, status });
  } catch (err) {
    console.error("Update request status error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

// ============================================
// GET /api/realmeet/notifications — Community Notifications List
// ============================================
router.get("/notifications", async (req, res) => {
  try {
    const rows = await queryMany(
      `SELECT n.*,
              u.name as triggering_user_name,
              u.avatar as triggering_user_avatar,
              u.photo_url as triggering_user_photo_url,
              cp.type as post_type,
              cp.purpose as post_title
       FROM notifications n
       LEFT JOIN users u ON u.id = n.triggering_user_id
       LEFT JOIN community_posts cp ON cp.id = n.community_post_id
       WHERE n.user_id = $1 AND n.type IN ('realmeet_request', 'realmeet_accepted', 'party_announcement')
       ORDER BY n.created_at DESC`,
      [req.user.userId]
    );

    const notifications = [];
    for (let r of rows) {
      // Find friendship status for requests
      let friendRequestId = null;
      let friendshipStatus = null;
      if (r.type === "realmeet_request") {
        const reqObj = await queryOne(
          "SELECT id, status FROM community_requests WHERE post_id = $1 AND applicant_user_id = $2",
          [r.community_post_id, r.triggering_user_id]
        );
        if (reqObj) {
          friendRequestId = reqObj.id;
          friendshipStatus = reqObj.status;
        }
      }

      notifications.push({
        id: r.id,
        type: r.type,
        read: r.read,
        triggeringUser: r.triggering_user_id ? {
          id: r.triggering_user_id,
          name: r.triggering_user_name,
          avatar: r.triggering_user_avatar,
          photoUrl: r.triggering_user_photo_url
        } : null,
        postId: r.community_post_id,
        postTitle: r.post_title,
        postType: r.post_type,
        friendRequestId,
        friendshipStatus,
        createdAt: new Date(r.created_at).getTime()
      });
    }

    res.json({ ok: true, notifications });
  } catch (err) {
    console.error("Get community notifications error:", err);
    res.status(500).json({ ok: false, error: "Internal server error" });
  }
});

module.exports = router;
