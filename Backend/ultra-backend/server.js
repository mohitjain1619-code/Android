require("dotenv").config();
const crypto = require("crypto");

const express = require("express");
const http = require("http");
const { Server } = require("socket.io");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

// Config
const { healthCheck: dbHealthCheck, query } = require("./src/config/database");
const redisModule = require("./src/config/redis");
const { healthCheck: redisHealthCheck } = redisModule;

// Google Play Reviewer Bypass Config (limits are disabled during active review)
const REVIEW_MODE = process.env.REVIEW_MODE === "true" || true;
const DAILY_FREE_CALL_LIMIT = 5;

// Routes
const authRoutes = require("./src/routes/auth");
const userRoutes = require("./src/routes/users");
const postRoutes = require("./src/routes/posts");
const chatRoutes = require("./src/routes/chats");
const notificationRoutes = require("./src/routes/notifications");
const verificationRoutes = require("./src/routes/verification");
const friendRoutes = require("./src/routes/friends");
const affiliateRoutes = require("./src/routes/affiliate");
const adsRoutes = require("./src/routes/ads");

// Services
const { startCleanupCron } = require("./src/services/cleanup");

// ============================================
// EXPRESS APP SETUP
// ============================================
const app = express();

// Security
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(express.json({ limit: "5mb" }));

// Global rate limiter
app.set('trust proxy', 1);

const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 100,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Too many requests, slow down" },
});
app.use("/api/", limiter);

// ============================================
// HEALTH CHECK
// ============================================
app.get("/", (req, res) => {
  res.status(200).send("Ultra-Backend is running (VPS-Ready)");
});

app.get("/health", async (req, res) => {
  const dbOk = await dbHealthCheck();
  const redisOk = await redisHealthCheck();
  const status = dbOk && redisOk ? 200 : 503;
  res.status(status).json({
    status: status === 200 ? "healthy" : "degraded",
    postgres: dbOk ? "connected" : "disconnected",
    redis: redisOk ? "connected" : "disconnected",
    uptime: process.uptime(),
  });
});

// ============================================
// API ROUTES
// ============================================
app.use("/api/auth", authRoutes);
app.use("/api/users", userRoutes);
app.use("/api/posts", postRoutes);
app.use("/api/chats", chatRoutes);
app.use("/api/notifications", notificationRoutes);
app.use("/api/verify", verificationRoutes);
app.use("/api/friends", friendRoutes);
app.use("/api/affiliate", affiliateRoutes);
app.use("/api/ads", adsRoutes);
app.use("/api", adsRoutes);

// ============================================
// ICE SERVERS ENDPOINT (WebRTC)
// ============================================
app.get("/api/webrtc/ice", (req, res) => {
  const iceServers = [
    { urls: "stun:stun.l.google.com:19302" },
    { urls: "stun:stun1.l.google.com:19302" },
  ];

  const useTurn = req.query.useTurn === "true" || process.env.TURN_ENABLED === "true";

  if (useTurn && process.env.TURN_USER && process.env.TURN_PASS) {
    // Self-hosted Coturn
    const turnHost = process.env.DOMAIN || "localhost";
    iceServers.push(
      {
        urls: `turn:${turnHost}:${process.env.TURN_PORT || 3478}`,
        username: process.env.TURN_USER,
        credential: process.env.TURN_PASS,
      },
      {
        urls: `turn:${turnHost}:${process.env.TURN_PORT || 3478}?transport=tcp`,
        username: process.env.TURN_USER,
        credential: process.env.TURN_PASS,
      }
    );
  }

  res.json({ iceServers });
});

// ============================================
// HTTP SERVER + SOCKET.IO
// ============================================
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

// Helper: Log WebRTC call room logs to Postgres database
async function logCallIfNeeded(room, state) {
  try {
    if (state && state.start_time && state.participants && state.participants.length >= 2) {
      const durationSeconds = Math.round((Date.now() - state.start_time) / 1000);
      const [callerId, receiverId] = state.participants;
      
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
      if (uuidRegex.test(callerId) && uuidRegex.test(receiverId) && durationSeconds > 0) {
        await query(
          `INSERT INTO call_logs (room_id, caller_id, receiver_id, duration_seconds) 
           VALUES ($1, $2, $3, $4)`,
          [room, callerId, receiverId, durationSeconds]
        );
        console.log(`📞 [Call Logged] Room: ${room}, Caller: ${callerId}, Receiver: ${receiverId}, Duration: ${durationSeconds}s`);
      }
    }
  } catch (err) {
    console.error("❌ Error logging call to database:", err);
  }
}

// ============================================
// SOCKET EVENTS (Redis-backed)
// ============================================
io.on("connection", (socket) => {
  console.log("🔥 Connected:", socket.id);

  // REGISTER USER
  socket.on("register-user", async ({ uid }) => {
    await redisModule.setUserOnline(uid, socket.id);
    console.log("✅ User registered:", uid, "→", socket.id);
  });

  // JOIN QUEUE (Optimized, O(1) atomic matching)
  socket.on("join-queue", async ({ uid, gender, category }) => {
    // 1. Enforce Daily Call Limits if Review Mode is INACTIVE
    if (!REVIEW_MODE) {
      try {
        const user = await query("SELECT is_premium FROM users WHERE id = $1 LIMIT 1", [uid]);
        const isPremium = user && user.rows && user.rows[0] && user.rows[0].is_premium;

        if (!isPremium) {
          const callCountResult = await query(
            "SELECT COUNT(*)::integer as count FROM call_logs WHERE (caller_id = $1 OR receiver_id = $1) AND created_at >= NOW() - INTERVAL '24 hours'",
            [uid]
          );
          const callCount = (callCountResult && callCountResult.rows && callCountResult.rows[0] && callCountResult.rows[0].count) || 0;

          // Check rewarded ads watched in the last 24 hours to extend daily allowed calls limit
          const adCountResult = await query(
            "SELECT COUNT(*)::integer as count FROM rewarded_ad_logs WHERE user_id = $1 AND created_at >= NOW() - INTERVAL '24 hours'",
            [uid]
          );
          const adCount = (adCountResult && adCountResult.rows && adCountResult.rows[0] && adCountResult.rows[0].count) || 0;

          const allowedCalls = DAILY_FREE_CALL_LIMIT + adCount;

          if (callCount >= allowedCalls) {
            // Generate temporary secure autologin token
            const token = crypto.randomBytes(16).toString("hex");
            await redisModule.setAutologinToken(token, uid);

            // Construct autologin URL pointing to backend autologin endpoint
            const backendUrl = process.env.BACKEND_URL || "https://android-9t8m.onrender.com";
            const autologinUrl = `${backendUrl}/auth/autologin?token=${token}&redirect=pricing`;

            console.log(`🛑 Daily Limit exceeded for user ${uid} (${callCount}/${allowedCalls} calls). Emitting limit-exceeded.`);
            socket.emit("limit-exceeded", { autologinUrl });
            return;
          }
        }
      } catch (err) {
        console.error("Error validating call limits for user:", uid, err.message);
      }
    }

    await redisModule.setUserOnline(uid, socket.id);
    await redisModule.removeFromQueue(uid);
    
    console.log("➕ Queue request from user:", uid, "category:", category, "gender:", gender);
    
    const { myQueueKey, targetQueueKey } = redisModule.getQueueKeys(gender, category);
    let matchFound = false;
    let peerId = null;
    
    // Try to pop a candidate from target queue atomically
    while (true) {
      peerId = await redisModule.popFromQueue(targetQueueKey);
      if (!peerId) {
        break; // Target queue is empty
      }
      
      // Ignore self-popping in identical queues (e.g. gay/lesbian)
      if (peerId === uid) {
        continue;
      }
      
      // Verify candidate is still online
      const peerSocket = await redisModule.getUserSocket(peerId);
      if (peerSocket) {
        matchFound = true;
        break; // Found a valid matched user!
      } else {
        // Discard offline user and clean up their queue registry
        await redisModule.removeFromQueue(peerId);
      }
    }
    
    if (matchFound && peerId) {
      // Clean up queue lists for both users
      await redisModule.removeFromQueue(peerId);
      await redisModule.removeFromQueue(uid);
      
      console.log("💚 MATCH FOUND:", uid, "<>", peerId);
      
      const s1 = socket.id;
      const s2 = await redisModule.getUserSocket(peerId);
      
      if (s1) io.to(s1).emit("match-found", { peerId });
      if (s2) io.to(s2).emit("match-found", { peerId: uid });
    } else {
      // No match found, enqueue self
      await redisModule.addToQueue(uid, gender, category);
    }
  });

  // LEAVE QUEUE
  socket.on("leave-queue", async ({ uid }) => {
    await redisModule.removeFromQueue(uid);
    console.log("🚪 Removed:", uid);
  });

  // ============================================
  // CALL ROOM LOGIC (Redis-backed)
  // ============================================
  socket.on("join-call-room", async ({ room, uid }) => {
    await redisModule.setUserOnline(uid, socket.id);
    socket.join(room);

    let state = await redisModule.getRoomState(room);
    if (!state) {
      state = { users: [], ready: false };
    }

    if (!state.users.includes(uid)) {
      state.users.push(uid);
    }

    await redisModule.setRoomState(room, state);
    console.log("👥", uid, "joined", room);

    if (state.users.length === 2 && !state.ready) {
      state.ready = true;
      state.start_time = Date.now();
      state.participants = [...state.users];
      await redisModule.setRoomState(room, state);
      console.log("⚡ Both ready in room:", room);
      io.to(room).emit("peer-ready", { room });
    }
  });

  socket.on("leave-call-room", async ({ room, uid }) => {
    console.log("🚪", uid, "left room", room);
    socket.leave(room);
    
    // Notify peer that this user disconnected
    io.to(room).emit("peer-disconnected", { uid });

    const state = await redisModule.getRoomState(room);
    if (state) {
      state.users = state.users.filter((u) => u !== uid);
      if (state.users.length === 0) {
        await logCallIfNeeded(room, state);
        await redisModule.deleteRoom(room);
        console.log("🗑️ Room deleted:", room);
      } else {
        await redisModule.setRoomState(room, state);
      }
    }
  });

  socket.on("call-control", ({ room, type, enabled, senderId }) => {
    socket.to(room).emit("call-control", { type, enabled, senderId });
  });

  // ============================================
  // SIGNALING RELAY (unchanged)
  // ============================================
  socket.on("send-offer", async ({ to, offer }) => {
    const s = await redisModule.getUserSocket(to);
    console.log(`📤 Relaying offer to ${to} (socket: ${s})`);
    if (s) io.to(s).emit("receive-offer", { offer });
  });

  socket.on("send-answer", async ({ to, answer }) => {
    const s = await redisModule.getUserSocket(to);
    console.log(`📤 Relaying answer to ${to} (socket: ${s})`);
    if (s) io.to(s).emit("receive-answer", { answer });
  });

  socket.on("send-ice", async ({ to, candidate, sdpMid, sdpMLineIndex }) => {
    const s = await redisModule.getUserSocket(to);
    console.log(`📤 Relaying ICE candidate to ${to} (socket: ${s})`);
    if (s) io.to(s).emit("receive-ice", { candidate, sdpMid, sdpMLineIndex });
  });

  // ============================================
  // PRIVATE 1v1 CALL SIGNALING (unchanged logic)
  // ============================================
  socket.on("start-private-call", async ({ callerId, targetId, isVideo, room, callerName, callerAvatar }) => {
    console.log("📞 Private call from", callerId, "to", targetId);
    await redisModule.setUserOnline(callerId, socket.id);

    const targetSocket = await redisModule.getUserSocket(targetId);
    if (targetSocket) {
      io.to(targetSocket).emit("incoming-private-call", {
        callerId,
        callerName: callerName || "Unknown",
        callerAvatar: callerAvatar || "",
        isVideo,
        room,
      });
      console.log("✅ Incoming call sent to", targetId);
    } else {
      console.log("❌ Target user", targetId, "not connected");
      socket.emit("call-failed", { reason: "User offline" });
    }
  });

  socket.on("accept-private-call", async ({ callerId, room }) => {
    const callerSocket = await redisModule.getUserSocket(callerId);
    if (callerSocket) io.to(callerSocket).emit("private-call-accepted", { room });
  });

  socket.on("reject-private-call", async ({ callerId }) => {
    const callerSocket = await redisModule.getUserSocket(callerId);
    if (callerSocket) io.to(callerSocket).emit("private-call-rejected");
  });

  socket.on("end-private-call", async ({ targetId }) => {
    const targetSocket = await redisModule.getUserSocket(targetId);
    if (targetSocket) io.to(targetSocket).emit("private-call-ended");
  });

  socket.on("send-private-offer", async ({ targetId, offer, room }) => {
    const s = await redisModule.getUserSocket(targetId);
    if (s) io.to(s).emit("receive-private-offer", { offer, room });
  });

  socket.on("send-private-answer", async ({ targetId, answer, room }) => {
    const s = await redisModule.getUserSocket(targetId);
    if (s) io.to(s).emit("receive-private-answer", { answer, room });
  });

  socket.on("send-private-ice", async ({ targetId, candidate, sdpMid, sdpMLineIndex, room }) => {
    const s = await redisModule.getUserSocket(targetId);
    if (s) io.to(s).emit("receive-private-ice", { candidate, sdpMid, sdpMLineIndex, room });
  });

  // ============================================
  // DISCONNECT CLEANUP
  // ============================================
  socket.on("disconnect", async () => {
    // Find which user disconnected by checking all online users
    const allSockets = await redisModule.redis.hgetall("camverz:user_socket");
    let disconnectedUid = null;

    for (const [uid, sid] of Object.entries(allSockets)) {
      if (sid === socket.id) {
        disconnectedUid = uid;
        await redisModule.setUserOffline(uid);
        await redisModule.removeFromQueue(uid);
        break;
      }
    }

    // Clean up rooms
    if (disconnectedUid) {
      const rooms = await redisModule.getAllRooms();
      for (const [room, state] of Object.entries(rooms)) {
        if (state.users.includes(disconnectedUid)) {
          state.users = state.users.filter((u) => u !== disconnectedUid);
          io.to(room).emit("peer-disconnected", { uid: disconnectedUid });

          if (state.users.length === 0) {
            await logCallIfNeeded(room, state);
            await redisModule.deleteRoom(room);
            console.log("🗑️ Room auto-deleted on disconnect:", room);
          } else {
            await redisModule.setRoomState(room, state);
          }
        }
      }
    }

    console.log("❌ Disconnected:", socket.id);
  });
});

// ============================================
// START SERVER
// ============================================
const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
  console.log(`🚀 Camverz Backend running on port ${PORT}`);
  console.log(`📊 Environment: ${process.env.NODE_ENV || "development"}`);

  // Start cleanup cron
  startCleanupCron();
});

// FIX for OkHttp "stream was reset" / socket hang up errors
server.keepAliveTimeout = 65000;
server.headersTimeout = 66000;
