const { Pool } = require("pg");
const fs = require("fs");
const path = require("path");

const externalUrl = "postgresql://camverz:FEJW3XOpa2PAZdiEEiccBqo44dHoIp67@dpg-dapvvs5g1s2s73djjpsg-a.oregon-postgres.render.com/camverz_1h55";

const pool = new Pool({
  connectionString: externalUrl,
  ssl: {
    rejectUnauthorized: false
  }
});

async function main() {
  try {
    console.log("Connecting to remote Render PostgreSQL database...");
    const client = await pool.connect();
    console.log("✅ Successfully connected to remote DB!");

    console.log("Checking if users table exists...");
    const checkTable = await client.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'users'
      );
    `);
    const tableExists = checkTable.rows[0].exists;

    if (!tableExists) {
      console.log("🚀 Fresh database! Importing schema.sql...");
      const schemaSql = fs.readFileSync(path.join(__dirname, "db/schema.sql"), "utf8");
      await client.query(schemaSql);
      console.log("✅ base schema.sql executed successfully!");
    } else {
      console.log("Table 'users' already exists.");
    }

    console.log("Importing affiliate_schema.sql if needed...");
    const affiliateSql = fs.readFileSync(path.join(__dirname, "db/affiliate_schema.sql"), "utf8");
    await client.query(affiliateSql);
    console.log("✅ affiliate_schema.sql executed successfully!");

    // Run additional migrations from database.js
    console.log("Running additional migrations...");
    await client.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS has_free_trial BOOLEAN NOT NULL DEFAULT true,
      ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT false,
      ADD COLUMN IF NOT EXISTS plan_name TEXT,
      ADD COLUMN IF NOT EXISTS plan_is_ad_free BOOLEAN NOT NULL DEFAULT false,
      ADD COLUMN IF NOT EXISTS plan_expires_at TIMESTAMPTZ,
      ADD COLUMN IF NOT EXISTS plan_started_at TIMESTAMPTZ,
      ADD COLUMN IF NOT EXISTS sex_preference TEXT NOT NULL DEFAULT 'Straight';

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
      CREATE INDEX IF NOT EXISTS idx_user_devices_device_id ON user_devices(device_id);

      ALTER TABLE comments 
      ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES comments(id) ON DELETE CASCADE;

      CREATE TABLE IF NOT EXISTS comment_likes (
        comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
        user_id UUID REFERENCES users(id) ON DELETE CASCADE,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        PRIMARY KEY (comment_id, user_id)
      );

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

      CREATE TABLE IF NOT EXISTS saved_parties (
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        PRIMARY KEY (user_id, post_id)
      );

      CREATE TABLE IF NOT EXISTS party_announcements (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        party_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
        host_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        text TEXT NOT NULL,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );

      ALTER TABLE notifications 
      ADD COLUMN IF NOT EXISTS community_post_id UUID REFERENCES community_posts(id) ON DELETE CASCADE;

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
    `);
    console.log("✅ All migrations and tables created successfully on camverz-db-new-01!");

    // Check list of created tables.
    const res = await client.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' 
      ORDER BY table_name;
    `);
    console.log("\n📋 Final Table List in camverz-db-new-01:");
    res.rows.forEach(r => console.log(" - " + r.table_name));

    client.release();
    process.exit(0);
  } catch (err) {
    console.error("❌ Error running DB initialization:", err);
    process.exit(1);
  }
}

main();