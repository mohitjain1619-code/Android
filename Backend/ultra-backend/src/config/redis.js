const Redis = require("ioredis");

const redis = new Redis({
  host: process.env.REDIS_HOST || "localhost",
  port: parseInt(process.env.REDIS_PORT || "6379"),
  password: process.env.REDIS_PASSWORD || undefined,
  maxRetriesPerRequest: 3,
  retryStrategy(times) {
    const delay = Math.min(times * 200, 5000);
    return delay;
  },
  lazyConnect: false,
});

redis.on("connect", () => {
  console.log("✅ Redis connected");
});

redis.on("error", (err) => {
  console.error("❌ Redis error:", err.message);
});

// ============================================
// ONLINE USERS
// ============================================
const ONLINE_KEY = "camverz:online";
const USER_SOCKET_KEY = "camverz:user_socket";

async function setUserOnline(uid, socketId) {
  await redis.hset(USER_SOCKET_KEY, uid, socketId);
  await redis.sadd(ONLINE_KEY, uid);
}

async function setUserOffline(uid) {
  await redis.hdel(USER_SOCKET_KEY, uid);
  await redis.srem(ONLINE_KEY, uid);
}

async function getUserSocket(uid) {
  return await redis.hget(USER_SOCKET_KEY, uid);
}

async function getOnlineCount() {
  return await redis.scard(ONLINE_KEY);
}

async function isUserOnline(uid) {
  return await redis.sismember(ONLINE_KEY, uid);
}

// ============================================
// MATCHMAKING QUEUE (Partitioned Sets)
// ============================================
const USER_QUEUE_KEY = "camverz:user_queue_key";

function getQueueKeys(gender, category) {
  let myQueueKey = "";
  let targetQueueKey = "";

  if (category === "straight") {
    myQueueKey = `camverz:queue:straight:${gender}`;
    targetQueueKey = `camverz:queue:straight:${gender === "male" ? "female" : "male"}`;
  } else if (category === "gay") {
    myQueueKey = "camverz:queue:gay";
    targetQueueKey = "camverz:queue:gay";
  } else if (category === "lesbian") {
    myQueueKey = "camverz:queue:lesbian";
    targetQueueKey = "camverz:queue:lesbian";
  } else {
    // Fallback for general matching
    myQueueKey = "camverz:queue:general";
    targetQueueKey = "camverz:queue:general";
  }

  return { myQueueKey, targetQueueKey };
}

async function addToQueue(uid, gender, category) {
  const { myQueueKey } = getQueueKeys(gender, category);
  
  // Track which queue key this user is inside
  await redis.hset(USER_QUEUE_KEY, uid, myQueueKey);
  // Add user to their partitioned queue set
  await redis.sadd(myQueueKey, uid);
}

async function removeFromQueue(uid) {
  const myQueueKey = await redis.hget(USER_QUEUE_KEY, uid);
  if (myQueueKey) {
    await redis.srem(myQueueKey, uid);
  }
  await redis.hdel(USER_QUEUE_KEY, uid);
}

async function popFromQueue(queueKey) {
  return await redis.spop(queueKey);
}

// ============================================
// ROOM STATE
// ============================================
const ROOM_KEY = "camverz:rooms";

async function setRoomState(room, state) {
  await redis.hset(ROOM_KEY, room, JSON.stringify(state));
}

async function getRoomState(room) {
  const data = await redis.hget(ROOM_KEY, room);
  return data ? JSON.parse(data) : null;
}

async function deleteRoom(room) {
  await redis.hdel(ROOM_KEY, room);
}

async function getAllRooms() {
  const all = await redis.hgetall(ROOM_KEY);
  const rooms = {};
  for (const [key, val] of Object.entries(all)) {
    rooms[key] = JSON.parse(val);
  }
  return rooms;
}

// ============================================
// RATE LIMITING
// ============================================
async function checkRateLimit(key, maxRequests, windowSeconds) {
  const current = await redis.incr(key);
  if (current === 1) {
    await redis.expire(key, windowSeconds);
  }
  return current <= maxRequests;
}

// ============================================
// HEALTH CHECK
// ============================================
async function healthCheck() {
  try {
    await redis.ping();
    return true;
  } catch {
    return false;
  }
}

// ============================================
// AUTOLOGIN TOKENS
// ============================================
async function setAutologinToken(token, userId) {
  const key = `camverz:autologin:${token}`;
  await redis.set(key, userId, "EX", 300); // Expires in 5 minutes (300 seconds)
}

async function getAutologinUser(token) {
  const key = `camverz:autologin:${token}`;
  const userId = await redis.get(key);
  if (userId) {
    await redis.del(key); // One-time use token
  }
  return userId;
}

module.exports = {
  redis,
  setUserOnline,
  setUserOffline,
  getUserSocket,
  getOnlineCount,
  isUserOnline,
  addToQueue,
  removeFromQueue,
  getQueueKeys,
  popFromQueue,
  setRoomState,
  getRoomState,
  deleteRoom,
  getAllRooms,
  checkRateLimit,
  setAutologinToken,
  getAutologinUser,
  healthCheck,
};
