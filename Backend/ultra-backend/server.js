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
const realMeetRoutes = require("./src/routes/realmeet");
const storiesRoutes = require("./src/routes/stories");

// Services
const { startCleanupCron } = require("./src/services/cleanup");

// ============================================
// EXPRESS APP SETUP
// ============================================
const app = express();

// Security
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());

// IMPORTANT: Razorpay webhook needs raw body for HMAC signature verification
// Must be registered BEFORE express.json() to capture raw bytes on this route
app.use("/api/affiliate/webhook/razorpay", express.raw({ type: "application/json" }), (req, res, next) => {
  // Attach rawBody string for signature verification, then parse JSON into req.body
  try {
    req.rawBody = req.body.toString("utf8");
    req.body = JSON.parse(req.rawBody);
  } catch (e) {
    return res.status(400).json({ error: "Invalid JSON body" });
  }
  next();
});

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
// HEALTH CHECK & REAL MEET COMMUNITY ROUTES
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
app.use("/api/realmeet", realMeetRoutes);
app.use("/api/stories", storiesRoutes);
app.use("/api", adsRoutes);

// Optimized Static Media Serving (Stories, verification, profile attachments)
const path = require("path");
app.use("/uploads", express.static(path.join(__dirname, "uploads"), {
  maxAge: "365d",
  setHeaders: (res, path) => {
    res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
  }
}));

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

// Make io accessible from Express routes (for emitting socket events from REST endpoints)
app.set("io", io);
app.set("redisModule", redisModule);

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
    socket.uid = uid; // Save on socket object for O(1) disconnect lookup
    socket.isSearching = false;
    socket.peerUid = null;
    socket.currentRoom = null;
    await redisModule.setUserOnline(uid, socket.id);
    console.log("✅ User registered:", uid, "→", socket.id);
  });

  // JOIN QUEUE (Optimized, O(1) atomic matching)
  socket.on("join-queue", async ({ uid, gender, category }) => {
    socket.uid = uid; // Save on socket object for O(1) disconnect lookup
    socket.isSearching = true; // Mark as actively matching
    socket.peerUid = null;
    socket.currentRoom = null;

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
      
      // Verify candidate is still online & actively searching
      const peerSocketId = await redisModule.getUserSocket(peerId);
      if (peerSocketId) {
        const peerSocket = io.sockets.sockets.get(peerSocketId);
        if (peerSocket && peerSocket.isSearching === true) {
          matchFound = true;
          break; // Found a valid matched user!
        }
      }
      
      // Discard invalid/stale/offline user and clean up their queue registry
      await redisModule.removeFromQueue(peerId);
    }
    
    if (matchFound && peerId) {
      // Clean up queue lists for both users
      await redisModule.removeFromQueue(peerId);
      await redisModule.removeFromQueue(uid);
      
      const s1 = socket.id;
      const s2 = await redisModule.getUserSocket(peerId);
      const peerSocket = io.sockets.sockets.get(s2);
      
      // Establish deterministic room name
      const room = uid.localeCompare(peerId) < 0 ? `${uid}_${peerId}` : `${peerId}_${uid}`;
      
      // Bind states to socket objects immediately to prevent race conditions during transitions
      socket.isSearching = false;
      socket.peerUid = peerId;
      socket.currentRoom = room;
      
      if (peerSocket) {
        peerSocket.isSearching = false;
        peerSocket.peerUid = uid;
        peerSocket.currentRoom = room;
      }
      
      console.log("💚 MATCH FOUND:", uid, "<>", peerId);
      if (s1) io.to(s1).emit("match-found", { peerId });
      if (s2) io.to(s2).emit("match-found", { peerId: uid });
    } else {
      // No match found, enqueue self
      await redisModule.addToQueue(uid, gender, category);
    }
  });

  // LEAVE QUEUE
  socket.on("leave-queue", async ({ uid }) => {
    socket.isSearching = false;
    socket.peerUid = null;
    socket.currentRoom = null;
    await redisModule.removeFromQueue(uid);
    console.log("🚪 Removed:", uid);
  });

  // ============================================
  // CALL ROOM LOGIC (Redis-backed)
  // ============================================
  socket.on("join-call-room", async ({ room, uid }) => {
    socket.uid = uid; // Save on socket object for O(1) disconnect lookup
    socket.currentRoom = room; // Track current call room for O(1) room cleanup
    socket.isSearching = false; // Disable search mode
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

    // If both participants are in the room, notify ready status (auto-retry compatible)
    if (state.users.length === 2) {
      state.ready = true;
      state.start_time = state.start_time || Date.now();
      state.participants = [...state.users];
      await redisModule.setRoomState(room, state);
      console.log("⚡ Both ready in room:", room);
      io.to(room).emit("peer-ready", { room });
    }
  });

  socket.on("leave-call-room", async ({ room, uid }) => {
    console.log("🚪", uid, "left room", room);
    socket.currentRoom = null; // Clear room tracking
    socket.peerUid = null;
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
    socket.uid = callerId; // Save on socket object for O(1) disconnect lookup
    await redisModule.setUserOnline(callerId, socket.id);

    // Check if callee is already in a call
    const calleeCallSession = await redisModule.redis.get(`camverz:call_session:${targetId}`);
    if (calleeCallSession) {
      console.log("📵 Target user", targetId, "is busy (already in a call)");
      socket.emit("call-failed", { reason: "User is busy on another call" });
      return;
    }

    const targetSocket = await redisModule.getUserSocket(targetId);
    if (targetSocket) {
      // Track active ringing session in Redis with 60s TTL (auto-cleanup)
      const sessionData = JSON.stringify({ callerId, targetId, room, isVideo, status: "ringing" });
      await redisModule.redis.set(`camverz:call_session:${callerId}`, sessionData, "EX", 60);
      await redisModule.redis.set(`camverz:call_session:${targetId}`, sessionData, "EX", 60);
      // Track which socket is the caller for disconnect cleanup
      await redisModule.redis.set(`camverz:call_socket:${socket.id}`, JSON.stringify({ callerId, targetId, room }), "EX", 60);

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
    // Verify the call session is still active (not cancelled)
    const sessionRaw = await redisModule.redis.get(`camverz:call_session:${callerId}`);
    if (!sessionRaw) {
      console.log("⚠️ Stale accept — call session no longer exists for caller", callerId);
      socket.emit("call-failed", { reason: "Call was cancelled" });
      return;
    }
    // Update session status to 'connected'
    try {
      const session = JSON.parse(sessionRaw);
      session.status = "connected";
      const connectedData = JSON.stringify(session);
      await redisModule.redis.set(`camverz:call_session:${callerId}`, connectedData, "EX", 3600);
      await redisModule.redis.set(`camverz:call_session:${session.targetId}`, connectedData, "EX", 3600);
    } catch (e) { /* ignore parse errors */ }

    const callerSocket = await redisModule.getUserSocket(callerId);
    if (callerSocket) io.to(callerSocket).emit("private-call-accepted", { room });
  });

  socket.on("reject-private-call", async ({ callerId }) => {
    // Clean up call session
    const sessionRaw = await redisModule.redis.get(`camverz:call_session:${callerId}`);
    if (sessionRaw) {
      try {
        const session = JSON.parse(sessionRaw);
        await redisModule.redis.del(`camverz:call_session:${session.callerId}`);
        await redisModule.redis.del(`camverz:call_session:${session.targetId}`);
      } catch (e) { /* ignore */ }
    }
    const callerSocket = await redisModule.getUserSocket(callerId);
    if (callerSocket) io.to(callerSocket).emit("private-call-rejected");
  });

  // Cancel a call before it's answered (caller hangs up during ringing)
  socket.on("cancel-private-call", async ({ targetId }) => {
    console.log("🚫 Call cancelled by caller, notifying", targetId);
    // Clean up call session
    const sessionRaw = await redisModule.redis.get(`camverz:call_session:${targetId}`);
    if (sessionRaw) {
      try {
        const session = JSON.parse(sessionRaw);
        await redisModule.redis.del(`camverz:call_session:${session.callerId}`);
        await redisModule.redis.del(`camverz:call_session:${session.targetId}`);
      } catch (e) { /* ignore */ }
    }
    const targetSocket = await redisModule.getUserSocket(targetId);
    if (targetSocket) io.to(targetSocket).emit("private-call-cancelled");
  });

  socket.on("end-private-call", async ({ targetId }) => {
    // Clean up call session
    const sessionRaw = await redisModule.redis.get(`camverz:call_session:${targetId}`);
    if (sessionRaw) {
      try {
        const session = JSON.parse(sessionRaw);
        await redisModule.redis.del(`camverz:call_session:${session.callerId}`);
        await redisModule.redis.del(`camverz:call_session:${session.targetId}`);
      } catch (e) { /* ignore */ }
    }
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
    let disconnectedUid = socket.uid;

    if (!disconnectedUid) {
      // Fallback O(N) lookup in case user disconnected before registration/binding
      try {
        const allSockets = await redisModule.redis.hgetall("camverz:user_socket");
        for (const [uid, sid] of Object.entries(allSockets)) {
          if (sid === socket.id) {
            disconnectedUid = uid;
            break;
          }
        }
      } catch (err) {
        console.error("Error during fallback disconnect lookup:", err);
      }
    }

    if (disconnectedUid) {
      try {
        await redisModule.setUserOffline(disconnectedUid);
        await redisModule.removeFromQueue(disconnectedUid);
      } catch (err) {
        console.error("Error setting user offline:", err);
      }
    }

    // Auto-cancel any pending ringing call session on disconnect
    try {
      const callSocketRaw = await redisModule.redis.get(`camverz:call_socket:${socket.id}`);
      if (callSocketRaw) {
        const { callerId, targetId, room } = JSON.parse(callSocketRaw);
        const sessionRaw = await redisModule.redis.get(`camverz:call_session:${callerId}`);
        if (sessionRaw) {
          const session = JSON.parse(sessionRaw);
          if (session.status === "ringing") {
            // Caller disconnected during ringing — cancel the call for callee
            const targetSocket = await redisModule.getUserSocket(targetId);
            if (targetSocket) {
              io.to(targetSocket).emit("private-call-cancelled");
              console.log("🚫 Auto-cancelled ringing call from", callerId, "to", targetId, "due to disconnect");
            }
            await redisModule.redis.del(`camverz:call_session:${callerId}`);
            await redisModule.redis.del(`camverz:call_session:${targetId}`);
          }
        }
        await redisModule.redis.del(`camverz:call_socket:${socket.id}`);
      }
    } catch (err) {
      console.error("Error during call session disconnect cleanup:", err);
    }

    // Clean up call session for this user
    if (disconnectedUid) {
      try {
        await redisModule.redis.del(`camverz:call_session:${disconnectedUid}`);
      } catch (e) { /* ignore */ }
    }

    // Direct peer notify on disconnect (immediate fallback)
    if (socket.peerUid) {
      try {
        const peerSocketId = await redisModule.getUserSocket(socket.peerUid);
        if (peerSocketId) {
          io.to(peerSocketId).emit("peer-disconnected", { uid: disconnectedUid });
          // Reset peer status on the active peer's socket
          const peerSocket = io.sockets.sockets.get(peerSocketId);
          if (peerSocket) {
            peerSocket.peerUid = null;
            peerSocket.currentRoom = null;
          }
        }
      } catch (err) {
        console.error("Error notifying peer on disconnect:", err);
      }
    }

    // Clean up rooms (O(1) room cleanup instead of O(M) rooms iteration)
    const currentRoom = socket.currentRoom;
    if (disconnectedUid && currentRoom) {
      try {
        const state = await redisModule.getRoomState(currentRoom);
        if (state && state.users.includes(disconnectedUid)) {
          state.users = state.users.filter((u) => u !== disconnectedUid);
          io.to(currentRoom).emit("peer-disconnected", { uid: disconnectedUid });

          if (state.users.length === 0) {
            await logCallIfNeeded(currentRoom, state);
            await redisModule.deleteRoom(currentRoom);
            console.log("🗑️ Room auto-deleted on disconnect:", currentRoom);
          } else {
            await redisModule.setRoomState(currentRoom, state);
          }
        }
      } catch (err) {
        console.error("Error cleaning up room on disconnect:", err);
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
