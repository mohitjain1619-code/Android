const cron = require("node-cron");
const { query, queryMany } = require("../config/database");

// ============================================
// Cleanup expired posts (runs every 5 minutes)
// Replaces: Firebase Cloud Functions + /cleanup/expired-posts endpoint
// ============================================
function startCleanupCron() {
  // Every 5 minutes
  cron.schedule("*/5 * * * *", async () => {
    try {
      const result = await query("DELETE FROM posts WHERE expiry_at <= NOW() RETURNING id");
      if (result.rowCount > 0) {
        console.log(`🧹 Cleaned up ${result.rowCount} expired posts`);
      }
    } catch (err) {
      console.error("Expired posts cleanup error:", err);
    }
  });

  console.log("✅ Cleanup cron started (every 5 min)");
}

// ============================================
// Manual cleanup of a user's data
// PostgreSQL ON DELETE CASCADE handles most of it automatically.
// This is for additional cleanup if needed.
// ============================================
async function cleanupUserData(userId) {
  console.log(`🧹 Starting cleanup for user ${userId}`);
  
  try {
    // PostgreSQL CASCADE handles:
    // - posts (and their likes, comments, notifications)
    // - messages (via chats)
    // - follows (both directions)
    // - friend_requests (both directions)
    // - blocked_users (both directions)
    // - reports
    // - notifications (both as user and as triggering_user)
    
    await query("DELETE FROM users WHERE id = $1", [userId]);
    console.log(`✅ Cleanup completed for user ${userId}`);
    return true;
  } catch (err) {
    console.error(`Error cleaning up user ${userId}:`, err);
    return false;
  }
}

module.exports = { startCleanupCron, cleanupUserData };
