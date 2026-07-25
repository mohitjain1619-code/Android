const { Pool } = require("pg");

const pool = new Pool({
  host: process.env.POSTGRES_HOST || "localhost",
  port: parseInt(process.env.POSTGRES_PORT || "5432"),
  database: process.env.POSTGRES_DB || "camverz",
  user: process.env.POSTGRES_USER || "camverz",
  password: process.env.POSTGRES_PASSWORD || "camverz_dev_2024",
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

// Test connection on startup
pool.on("connect", () => {
  console.log("✅ PostgreSQL connected");
});

pool.on("error", (err) => {
  console.error("❌ PostgreSQL pool error:", err);
});

// Helper: execute a query
async function query(text, params) {
  const start = Date.now();
  const res = await pool.query(text, params);
  const duration = Date.now() - start;
  if (process.env.NODE_ENV === "development" && duration > 100) {
    console.log(`⚠️ Slow query (${duration}ms):`, text.substring(0, 80));
  }
  return res;
}

// Helper: get a single row
async function queryOne(text, params) {
  const res = await query(text, params);
  return res.rows[0] || null;
}

// Helper: get multiple rows
async function queryMany(text, params) {
  const res = await query(text, params);
  return res.rows;
}

// Health check
async function healthCheck() {
  try {
    await pool.query("SELECT 1");
    return true;
  } catch {
    return false;
  }
}

module.exports = { pool, query, queryOne, queryMany, healthCheck };
