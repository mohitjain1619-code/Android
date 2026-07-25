const jwt = require("jsonwebtoken");

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
function requireAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;

    if (!token) {
      return res.status(401).json({ error: "Missing Authorization Bearer token" });
    }

    const decoded = verifyToken(token);
    req.user = {
      userId: decoded.userId,
      email: decoded.email,
      googleId: decoded.googleId,
    };
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
function optionalAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;

    if (token) {
      const decoded = verifyToken(token);
      req.user = {
        userId: decoded.userId,
        email: decoded.email,
        googleId: decoded.googleId,
      };
    }
  } catch {
    // Ignore errors for optional auth
  }
  next();
}

module.exports = { generateToken, verifyToken, requireAuth, optionalAuth };
