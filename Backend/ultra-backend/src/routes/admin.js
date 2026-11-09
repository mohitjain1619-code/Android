const express = require("express");
const router = express.Router();
const { query } = require("../config/database");
const { requireAuth } = require("../middleware/auth");

const ADMIN_EMAIL = "mohitjain1619@gmail.com";

// Strict Admin Middleware
const requireAdmin = (req, res, next) => {
  if (!req.user || !req.user.email || req.user.email.toLowerCase() !== ADMIN_EMAIL) {
    return res.status(403).json({ error: "Access Denied: Only mohitjain1619@gmail.com is authorized." });
  }
  next();
};

/**
 * Direct Reset Endpoint (can be triggered directly in browser tab with secret key)
 * GET /api/admin/direct-reset-user?email=mohitjain1619@gmail.com&key=camverz_reset_2026
 */
router.get("/direct-reset-user", async (req, res) => {
  try {
    const { email, key } = req.query;
    if (key !== "camverz_reset_2026") {
      return res.status(403).json({ error: "Invalid secret key" });
    }
    const targetEmail = (email || ADMIN_EMAIL).trim().toLowerCase();
    const userRes = await query("SELECT id, email FROM users WHERE LOWER(email) = LOWER($1)", [targetEmail]);
    const user = userRes.rows[0];

    if (!user) {
      return res.send(`
        <html>
          <body style="background:#0b1115;color:#54d6d2;font-family:sans-serif;text-align:center;padding:50px;">
            <h1 style="color:#fbbf24;">ℹ️ User Already Clean</h1>
            <p>Account <b>${targetEmail}</b> does not exist in the database (already completely fresh).</p>
            <p>You can now install the app and sign in with Google to test onboarding!</p>
          </body>
        </html>
      `);
    }

    await query("DELETE FROM user_devices WHERE user_id = $1", [user.id]).catch(() => {});
    await query("DELETE FROM user_journey_events WHERE user_id = $1 OR LOWER(user_email) = LOWER($2)", [user.id, targetEmail]).catch(() => {});
    await query("DELETE FROM users WHERE id = $1", [user.id]);

    console.log(`🧹 [Admin Direct Reset] Deleted ${targetEmail} (${user.id})`);
    return res.send(`
      <html>
        <body style="background:#0b1115;color:#54d6d2;font-family:sans-serif;text-align:center;padding:50px;">
          <h1 style="color:#34d399;">✅ User Deleted Successfully</h1>
          <p>Account <b>${targetEmail}</b> has been completely removed from the server.</p>
          <p>Ab aap app me Google Sign-In karenge to <b>Brand New User</b> bankar pura fresh onboarding start hoga!</p>
        </body>
      </html>
    `);
  } catch (err) {
    console.error("Direct reset error:", err);
    return res.status(500).json({ error: err.message });
  }
});

router.use(requireAuth, requireAdmin);

/**
 * Format UTC Date to IST String
 */
function toISTString(date) {
  if (!date) return "-";
  return new Date(date).toLocaleString("en-IN", {
    timeZone: "Asia/Kolkata",
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: true
  });
}

/**
 * GET /api/admin/overview
 * Realtime KPIs, Onboarding Funnel, Active Devices, Play Store Metrics
 */
router.get("/overview", async (req, res) => {
  try {
    const { date } = req.query; // YYYY-MM-DD (optional, default: all-time or today)
    let dateFilter = "";
    const params = [];

    if (date) {
      dateFilter = `WHERE created_at::date = $1`;
      params.push(date);
    }

    // 1. Total Unique Devices & Users
    const totalsRes = await query(`
      SELECT 
        COUNT(DISTINCT device_id) as total_devices,
        COUNT(DISTINCT user_email) FILTER (WHERE user_email IS NOT NULL) as total_signed_users,
        COUNT(DISTINCT device_id) FILTER (WHERE is_play_store = true) as play_store_devices,
        COUNT(DISTINCT device_id) FILTER (WHERE is_play_store = false) as other_installs
      FROM user_journey_events
      ${dateFilter}
    `, params);

    // 2. Onboarding Funnel Breakdown
    const funnelRes = await query(`
      SELECT 
        COUNT(DISTINCT device_id) FILTER (WHERE event_name IN ('APP_OPENED', 'APP_INSTALLED')) as step_app_open,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'GOOGLE_SIGN_IN_SUCCESS') as step_auth_success,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'GOOGLE_SIGN_IN_FAILED') as step_auth_failed,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'ONBOARDING_GENDER_SET') as step_gender_set,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'ONBOARDING_DOB_SET') as step_dob_set,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'LOCATION_PERMISSION_GRANTED') as step_loc_granted,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'LOCATION_PERMISSION_DENIED') as step_loc_denied,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'ONBOARDING_COMPLETED') as step_completed,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name IN ('CALL_INITIATED', 'CALL_CONNECTED')) as step_made_call,
        COUNT(DISTINCT device_id) FILTER (WHERE event_name = 'POST_PUBLISHED') as step_made_post
      FROM user_journey_events
      ${dateFilter}
    `, params);

    // 3. Ad Flow Breakdown
    const adsRes = await query(`
      SELECT 
        COUNT(*) as total_events,
        COUNT(*) FILTER (WHERE event_name = 'AD_REQUESTED') as total_requests,
        COUNT(*) FILTER (WHERE event_name IN ('AD_IMPRESSION', 'AD_COMPLETED')) as total_delivered,
        COUNT(*) FILTER (WHERE event_name = 'AD_FAILED') as total_failed,
        COUNT(*) FILTER (WHERE (details->>'format') = 'rewarded' OR (details->>'ad_type') = 'rewarded') as rewarded_count,
        COUNT(*) FILTER (WHERE (details->>'format') = 'interstitial' OR (details->>'ad_type') = 'interstitial') as interstitial_count,
        COALESCE(SUM((details->>'revenue')::float), 0) as total_revenue
      FROM user_journey_events
      WHERE category = 'AD_EVENT' ${date ? `AND created_at::date = $1` : ""}
    `, date ? [date] : []);

    // 4. Recent Active Users (Last 30 Mins)
    const activeRes = await query(`
      SELECT COUNT(DISTINCT device_id) as active_devices_30m
      FROM user_journey_events
      WHERE created_at >= NOW() - INTERVAL '30 minutes'
    `);

    res.json({
      success: true,
      totals: totalsRes.rows[0] || {},
      funnel: funnelRes.rows[0] || {},
      ads: adsRes.rows[0] || {},
      active_last_30m: parseInt(activeRes.rows[0]?.active_devices_30m || "0", 10),
      server_time_ist: toISTString(new Date())
    });
  } catch (err) {
    console.error("❌ Admin Overview error:", err);
    res.status(500).json({ error: "Failed to load admin overview" });
  }
});

/**
 * GET /api/admin/users
 * Searchable, filterable list of user/device journeys
 */
router.get("/users", async (req, res) => {
  try {
    const { search, country, status, isPlayStore, limit = 50, offset = 0 } = req.query;

    let conditions = ["1=1"];
    const params = [];

    if (search) {
      params.push(`%${search.trim()}%`);
      conditions.push(`(u.user_email ILIKE $${params.length} OR u.user_name ILIKE $${params.length} OR u.device_id ILIKE $${params.length})`);
    }

    if (country) {
      params.push(`%${country.trim()}%`);
      conditions.push(`(u.device_info->>'country' ILIKE $${params.length} OR u.details->>'country' ILIKE $${params.length})`);
    }

    if (isPlayStore !== undefined && isPlayStore !== "") {
      params.push(isPlayStore === "true");
      conditions.push(`u.is_play_store = $${params.length}`);
    }

    const whereClause = conditions.join(" AND ");

    const usersQuery = `
      WITH UserSummary AS (
        SELECT 
          device_id,
          MAX(user_email) as user_email,
          MAX(user_name) as user_name,
          MAX(is_play_store::int)::boolean as is_play_store,
          MAX(device_info->>'model') as device_model,
          MAX(device_info->>'android') as android_version,
          MAX(COALESCE(device_info->>'country', details->>'country')) as country,
          MIN(created_at) as first_seen,
          MAX(created_at) as last_seen,
          COUNT(*) as total_events,
          BOOL_OR(event_name = 'ONBOARDING_COMPLETED') as onboarding_completed,
          BOOL_OR(event_name = 'LOCATION_PERMISSION_DENIED') as has_loc_denied,
          BOOL_OR(category = 'ERROR' OR event_name LIKE '%FAILED%') as has_errors
        FROM user_journey_events u
        WHERE ${whereClause}
        GROUP BY device_id
      )
      SELECT * FROM UserSummary
      ORDER BY last_seen DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2}
    `;

    params.push(parseInt(limit, 10));
    params.push(parseInt(offset, 10));

    const result = await query(usersQuery, params);

    const formatted = result.rows.map(row => ({
      ...row,
      first_seen_ist: toISTString(row.first_seen),
      last_seen_ist: toISTString(row.last_seen),
      status: row.onboarding_completed ? "COMPLETED" : (row.has_errors ? "HAS_ERRORS" : "DROPPED_OFF")
    }));

    res.json({ success: true, users: formatted });
  } catch (err) {
    console.error("❌ Admin Users error:", err);
    res.status(500).json({ error: "Failed to load user list" });
  }
});

/**
 * GET /api/admin/user-journey/:identifier
 * Detailed chronological timeline in IST for a specific device or user email
 */
router.get("/user-journey/:identifier", async (req, res) => {
  try {
    const { identifier } = req.params;
    if (!identifier) return res.status(400).json({ error: "Identifier required" });

    const eventsRes = await query(`
      SELECT 
        id,
        device_id,
        user_id,
        user_email,
        user_name,
        is_play_store,
        category,
        event_name,
        details,
        device_info,
        created_at
      FROM user_journey_events
      WHERE device_id = $1 OR user_email = $1 OR user_id::text = $1
      ORDER BY created_at ASC
    `, [identifier]);

    if (!eventsRes.rows || eventsRes.rows.length === 0) {
      return res.status(404).json({ error: "No telemetry events found for this identifier" });
    }

    const firstTime = eventsRes.rows[0].created_at;
    let prevTime = firstTime;

    const timeline = eventsRes.rows.map((row, idx) => {
      const current = new Date(row.created_at);
      const deltaFromStartSec = Math.round((current - new Date(firstTime)) / 1000);
      const deltaFromPrevSec = Math.round((current - new Date(prevTime)) / 1000);
      prevTime = current;

      return {
        id: row.id,
        category: row.category,
        event_name: row.event_name,
        details: row.details || {},
        device_info: row.device_info || {},
        created_at_utc: row.created_at,
        created_at_ist: toISTString(row.created_at),
        step_number: idx + 1,
        seconds_from_start: deltaFromStartSec,
        seconds_from_prev: deltaFromPrevSec
      };
    });

    res.json({
      success: true,
      identifier,
      total_steps: timeline.length,
      device_info: eventsRes.rows[0].device_info || {},
      user_email: eventsRes.rows[eventsRes.rows.length - 1].user_email || null,
      user_name: eventsRes.rows[eventsRes.rows.length - 1].user_name || null,
      is_play_store: eventsRes.rows[0].is_play_store,
      timeline
    });
  } catch (err) {
    console.error("❌ Admin Timeline error:", err);
    res.status(500).json({ error: "Failed to load user journey" });
  }
});

/**
 * GET /api/admin/ad-flow
 * Detailed lifecycle of ad requests (Date-wise, user-wise, placement-wise)
 */
router.get("/ad-flow", async (req, res) => {
  try {
    const { date, placement, format, status, limit = 100 } = req.query;

    let conditions = ["category = 'AD_EVENT'"];
    const params = [];

    if (date) {
      params.push(date);
      conditions.push(`created_at::date = $${params.length}`);
    }

    if (placement) {
      params.push(`%${placement.trim()}%`);
      conditions.push(`(details->>'placement') ILIKE $${params.length}`);
    }

    if (format) {
      params.push(format.toLowerCase());
      conditions.push(`LOWER(COALESCE(details->>'format', details->>'ad_type', '')) = $${params.length}`);
    }

    if (status) {
      params.push(status.toUpperCase());
      conditions.push(`UPPER(COALESCE(details->>'status', event_name)) = $${params.length}`);
    }

    const adQuery = `
      SELECT 
        id,
        device_id,
        user_email,
        user_name,
        event_name,
        details,
        device_info,
        created_at
      FROM user_journey_events
      WHERE ${conditions.join(" AND ")}
      ORDER BY created_at DESC
      LIMIT $${params.length + 1}
    `;

    params.push(parseInt(limit, 10));

    const result = await query(adQuery, params);

    const formatted = result.rows.map(row => ({
      id: row.id,
      device_id: row.device_id,
      user_email: row.user_email || "Anonymous Guest",
      user_name: row.user_name || "New User",
      country: row.device_info?.country || row.details?.country || "IN",
      event_name: row.event_name,
      placement: row.details?.placement || "general",
      ad_format: row.details?.format || row.details?.ad_type || "rewarded",
      network: row.details?.network || row.details?.ad_network || "ironsource",
      revenue: row.details?.revenue !== undefined ? row.details?.revenue : null,
      status: row.details?.status || (row.event_name.includes("FAILED") ? "FAILED" : "DELIVERED"),
      error_message: row.details?.error_message || row.details?.error || "",
      created_at_ist: toISTString(row.created_at)
    }));

    res.json({ success: true, logs: formatted });
  } catch (err) {
    console.error("❌ Admin Ad Flow error:", err);
    res.status(500).json({ error: "Failed to load ad flow logs" });
  }
});

/**
 * POST /api/admin/reset-user
 * Deletes user account from users table and cascades related data so they can test fresh onboarding
 */
router.post("/reset-user", async (req, res) => {
  try {
    const targetEmail = (req.body && req.body.email) ? req.body.email.trim().toLowerCase() : ADMIN_EMAIL;
    const userRes = await query("SELECT id, email FROM users WHERE LOWER(email) = LOWER($1)", [targetEmail]);
    const user = userRes.rows[0];

    if (!user) {
      return res.json({ ok: true, message: `User ${targetEmail} is already fresh (not in database).` });
    }

    await query("DELETE FROM user_devices WHERE user_id = $1", [user.id]).catch(() => {});
    await query("DELETE FROM user_journey_events WHERE user_id = $1 OR LOWER(user_email) = LOWER($2)", [user.id, targetEmail]).catch(() => {});
    await query("DELETE FROM users WHERE id = $1", [user.id]);

    console.log(`🧹 [Admin Reset] Deleted ${targetEmail} (${user.id})`);
    return res.json({ ok: true, message: `Account ${targetEmail} successfully deleted. Next login will start fresh onboarding!` });
  } catch (err) {
    console.error("Reset user error:", err);
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;

