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

// Run automatic migrations
async function runMigrations() {
  try {
    console.log("🔄 Checking database migrations...");
    
    // 1. Add has_free_trial to users if not exists
    await pool.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS has_free_trial BOOLEAN NOT NULL DEFAULT true;
    `);
    
    // 2. Create user_devices table if not exists
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_devices (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        device_id TEXT NOT NULL,
        user_id UUID REFERENCES users(id) ON DELETE CASCADE,
        platform TEXT NOT NULL,
        ip_address TEXT DEFAULT '',
        user_agent TEXT DEFAULT '',
        first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        UNIQUE(device_id, user_id)
      );
    `);
    
    // 3. Create index on user_devices if not exists
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_user_devices_device_id ON user_devices(device_id);
    `);

    // 4. Add is_premium to users if not exists
    await pool.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT false;
    `);

    // 5. Add parent_id to comments for replies support
    await pool.query(`
      ALTER TABLE comments 
      ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES comments(id) ON DELETE CASCADE;
    `);

    // 6. Create comment_likes table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS comment_likes (
        comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
        user_id UUID REFERENCES users(id) ON DELETE CASCADE,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        PRIMARY KEY (comment_id, user_id)
      );
    `);

    // 7. Add subscription fields to users
    await pool.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS plan_name TEXT,
      ADD COLUMN IF NOT EXISTS plan_is_ad_free BOOLEAN NOT NULL DEFAULT false,
      ADD COLUMN IF NOT EXISTS plan_expires_at TIMESTAMPTZ,
      ADD COLUMN IF NOT EXISTS plan_started_at TIMESTAMPTZ;
    `);

    // 8. Add sex_preference to users
    await pool.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS sex_preference TEXT NOT NULL DEFAULT 'Straight';
    `);

    console.log("✅ Database migrations applied successfully!");
  } catch (err) {
    console.error("❌ Failed to apply database migrations:", err.message);
  }
}

// Trigger migrations
runMigrations();

module.exports = { pool, query, queryOne, queryMany, healthCheck };
