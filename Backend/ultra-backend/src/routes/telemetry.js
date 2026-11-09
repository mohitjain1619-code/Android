const express = require("express");
const router = express.Router();
const { query } = require("../config/database");

// Auto-create telemetry schema on startup
query(`
  CREATE TABLE IF NOT EXISTS user_journey_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    user_id UUID,
    user_email TEXT,
    user_name TEXT,
    is_play_store BOOLEAN DEFAULT true,
    category TEXT NOT NULL,
    event_name TEXT NOT NULL,
    details JSONB DEFAULT '{}'::jsonb,
    device_info JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  CREATE INDEX IF NOT EXISTS idx_uje_device_created ON user_journey_events (device_id, created_at DESC);
  CREATE INDEX IF NOT EXISTS idx_uje_email_created ON user_journey_events (user_email, created_at DESC);
  CREATE INDEX IF NOT EXISTS idx_uje_category_created ON user_journey_events (category, created_at DESC);
  CREATE INDEX IF NOT EXISTS idx_uje_event_created ON user_journey_events (event_name, created_at DESC);
`).then(() => {
  console.log("✅ [Telemetry] user_journey_events table & indexes verified");
}).catch(err => {
  console.error("❌ [Telemetry] Error initializing telemetry table:", err.message);
});

/**
 * POST /api/telemetry/log
 * Ingests single or batched journey/event logs from Android client
 */
router.post("/log", async (req, res) => {
  try {
    const rawEvents = Array.isArray(req.body.events) ? req.body.events : (req.body.event ? [req.body.event] : [req.body]);
    if (!rawEvents || rawEvents.length === 0 || !rawEvents[0].category) {
      return res.status(400).json({ error: "No valid telemetry events provided" });
    }

    const authUser = req.user || null;

    for (const ev of rawEvents) {
      if (!ev || !ev.category || !ev.eventName) continue;

      const deviceId = ev.deviceId || ev.device_id || "unknown_device";
      const userId = ev.userId || ev.user_id || (authUser ? authUser.userId : null) || null;
      const userEmail = ev.userEmail || ev.user_email || (authUser ? authUser.email : null) || null;
      const userName = ev.userName || ev.user_name || (authUser ? authUser.name : null) || null;
      const isPlayStore = ev.isPlayStore !== undefined ? Boolean(ev.isPlayStore) : true;
      const category = String(ev.category).toUpperCase();
      const eventName = String(ev.eventName).toUpperCase();
      const details = typeof ev.details === "object" && ev.details !== null ? ev.details : {};
      const deviceInfo = typeof ev.deviceInfo === "object" && ev.deviceInfo !== null ? ev.deviceInfo : {};

      await query(
        `INSERT INTO user_journey_events 
         (device_id, user_id, user_email, user_name, is_play_store, category, event_name, details, device_info, created_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, COALESCE($10, NOW()))`,
        [
          deviceId,
          userId,
          userEmail,
          userName,
          isPlayStore,
          category,
          eventName,
          JSON.stringify(details),
          JSON.stringify(deviceInfo),
          ev.timestamp ? new Date(ev.timestamp) : null
        ]
      );
    }

    return res.status(200).json({ success: true, count: rawEvents.length });
  } catch (err) {
    console.error("❌ Telemetry ingestion error:", err.message);
    return res.status(500).json({ error: "Failed to store telemetry" });
  }
});

module.exports = router;

