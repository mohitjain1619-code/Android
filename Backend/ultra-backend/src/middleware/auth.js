const jwt = require("jsonwebtoken");
const { queryOne } = require("../config/database");

const JWT_SECRET = process.env.JWT_SECRET || "camverz-jwt-super-secret-change-in-production-2024";

// Generate JWT for a user
function generateToken(user) {
  return jwt.sign(
    {
      userId: user.id,
      email: user.email,
      googleId: user.google_id,
    },
    JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRY || "30d" }
  );
}

// Verify JWT and extract payload
function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

// Middleware: require authentication
async function requireAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;

    if (!token) {
      return res.status(401).json({ error: "Missing Authorization Bearer token" });
    }

    const decoded = verifyToken(token);
    if (!decoded || !decoded.userId) {
      return res.status(401).json({ error: "Invalid token" });
    }

    // Verify user actually exists in database (rejects deleted account tokens)
    const userExists = await queryOne("SELECT id FROM users WHERE id = $1", [decoded.userId]);
    if (!userExists) {
      return res.status(401).json({ error: "account_deleted", message: "Account no longer exists. Please sign in again." });
    }

    req.user = {
      userId: decoded.userId,
      email: decoded.email,
      googleId: decoded.googleId,
    };

    // Asynchronously touch last_seen_at in user_devices for realtime active tracking (non-blocking)
    (async () => {
      try {
        const userAgent = req.headers["user-agent"] || "";
        const isMobileAgent = userAgent.toLowerCase().includes("okhttp") || userAgent.toLowerCase().includes("android");
        const platform = isMobileAgent ? "android" : "web";
        const deviceId = `${platform}-${decoded.userId}`;
        const clientIp = (req.headers["x-forwarded-for"] || req.socket.remoteAddress || "").split(",")[0].trim();

        await queryOne(
          `INSERT INTO user_devices (device_id, user_id, platform, ip_address, user_agent, last_seen_at)
           VALUES ($1, $2, $3, $4, $5, NOW())
           ON CONFLICT (device_id, user_id)
           DO UPDATE SET last_seen_at = NOW()`,
          [deviceId, decoded.userId, platform, clientIp, userAgent]
        );
      } catch (e) {}
    })();

    next();
  } catch (err) {
    if (err.name === "TokenExpiredError") {
      return res.status(401).json({ error: "Token expired" });
    }
    if (err.name === "JsonWebTokenError") {
      return res.status(401).json({ error: "Invalid token" });
    }
    return res.status(401).json({ error: "Authentication failed" });
  }
}

// Optional auth: sets req.user if token present, but doesn't fail
async function optionalAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;

    if (token) {
      const decoded = verifyToken(token);
      if (decoded && decoded.userId) {
        const userExists = await queryOne("SELECT id FROM users WHERE id = $1", [decoded.userId]);
        if (userExists) {
          req.user = {
            userId: decoded.userId,
            email: decoded.email,
            googleId: decoded.googleId,
          };
        }
      }
    }
    next();
  } catch (err) {
    next();
  }
}

module.exports = { generateToken, verifyToken, requireAuth, optionalAuth };
