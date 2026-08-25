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
    
    // Check if users table exists (to detect fresh database)
    const checkTable = await pool.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'users'
      );
    `);
    const tableExists = checkTable.rows[0].exists;

    if (!tableExists) {
      console.log("🚀 Fresh database detected! Initializing base schema from schema.sql...");
      const fs = require("fs");
      const path = require("path");
      const schemaPath = path.join(__dirname, "../../db/schema.sql");
      const schemaSql = fs.readFileSync(schemaPath, "utf8");
      
      // Execute the schema SQL
      await pool.query(schemaSql);
      console.log("✅ Base schema imported successfully!");
    }

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

    // 9. Add database speed indexes to prevent slow queries on active feeds
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_posts_expiry_at ON posts(expiry_at);
      CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at);
      CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
      CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
      CREATE INDEX IF NOT EXISTS idx_follows_follower_id ON follows(follower_id);
      CREATE INDEX IF NOT EXISTS idx_follows_following_id ON follows(following_id);
    `);

    // 10. Create Real Meet Community Posts Table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS community_posts (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        type TEXT NOT NULL,
        purpose TEXT DEFAULT '',
        location TEXT DEFAULT '',
        meeting_time TEXT DEFAULT '',
        description TEXT DEFAULT '',
        capacity INTEGER DEFAULT 0,
        target_gender TEXT DEFAULT '',
        relationship_status TEXT DEFAULT '',
        interests TEXT DEFAULT '',
        list_visibility TEXT NOT NULL DEFAULT 'PRIVATE',
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
      CREATE INDEX IF NOT EXISTS idx_community_posts_type ON community_posts(type);
      CREATE INDEX IF NOT EXISTS idx_community_posts_user_id ON community_posts(user_id);
    `);

    // 11. Create Real Meet Community Requests Table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS community_requests (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
        poster_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        applicant_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        message TEXT DEFAULT '',
        contact_preference TEXT DEFAULT '',
        status TEXT NOT NULL DEFAULT 'PENDING',
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        UNIQUE(post_id, applicant_user_id)
      );
      CREATE INDEX IF NOT EXISTS idx_community_requests_post_id ON community_requests(post_id);
      CREATE INDEX IF NOT EXISTS idx_community_requests_applicant ON community_requests(applicant_user_id);
    `);

    // 12. Create Saved Party Posts Table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS saved_parties (
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        PRIMARY KEY (user_id, post_id)
      );
    `);

    // 13. Create Host Announcements for Parties Table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS party_announcements (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        party_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
        host_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        text TEXT NOT NULL,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
    `);

    // 14. Add community_post_id to notifications
    await pool.query(`
      ALTER TABLE notifications 
      ADD COLUMN IF NOT EXISTS community_post_id UUID REFERENCES community_posts(id) ON DELETE CASCADE;
    `);

    // 15. Create Stories Table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS stories (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        type TEXT NOT NULL,
        media_url TEXT,
        text_content TEXT,
        text_color TEXT,
        bg_gradient TEXT,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours')
      );
      CREATE INDEX IF NOT EXISTS idx_stories_expires_at ON stories(expires_at);
    `);

    console.log("✅ Database migrations and speed indexing applied successfully!");
  } catch (err) {
    console.error("❌ Failed to apply database migrations or indexes:", err.message);
  }
}

// Trigger migrations
runMigrations();

module.exports = { pool, query, queryOne, queryMany, healthCheck };
