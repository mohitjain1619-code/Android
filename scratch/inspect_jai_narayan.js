const path = require('path');
const { Pool } = require(path.resolve(__dirname, '../Backend/ultra-backend/node_modules/pg'));

const pool = new Pool({
  connectionString: "postgresql://camverz:FEJW3XOpa2PAZdiEEiccBqo44dHoIp67@dpg-dapvvs5g1s2s73djjpsg-a.oregon-postgres.render.com/camverz_1h55",
  ssl: { rejectUnauthorized: false }
});

async function inspectUser() {
  try {
    const userId = 'e3b0bdb2-8930-4c09-b2e9-e51a82bb4ca2';
    console.log(`\n==============================================`);
    console.log(`EXACT DATABASE RECORDS FOR USER: ${userId}`);
    console.log(`==============================================\n`);

    const postsRes = await pool.query(`SELECT * FROM posts WHERE user_id = $1`, [userId]);
    console.log(`1. FEED POSTS (posts table) - Count: ${postsRes.rows.length}`);
    console.log(postsRes.rows);

    const commRes = await pool.query(`SELECT id, type, purpose, location, description, created_at FROM community_posts WHERE user_id = $1`, [userId]);
    console.log(`\n2. COMMUNITY POSTS (community_posts table) - Count: ${commRes.rows.length}`);
    console.log(commRes.rows);

    const storiesRes = await pool.query(`SELECT * FROM stories WHERE user_id = $1`, [userId]);
    console.log(`\n3. STORIES (stories table) - Count: ${storiesRes.rows.length}`);
    console.log(storiesRes.rows);

    const callsRes = await pool.query(`SELECT * FROM call_logs WHERE caller_id = $1 OR receiver_id = $1`, [userId]);
    console.log(`\n4. CALL LOGS (call_logs table) - Count: ${callsRes.rows.length}`);
    console.log(callsRes.rows);

    process.exit(0);
  } catch (err) {
    console.error("Error inspecting database:", err);
    process.exit(1);
  }
}

inspectUser();
