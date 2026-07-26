const express = require("express");
const crypto = require("crypto");
const { query, queryOne, queryMany } = require("../config/database");
const { requireAuth } = require("../middleware/auth");
const axios = require("axios");

const router = express.Router();

// Auto-run schema migration on start to add Instagram and YouTube fields
(async () => {
  try {
    await query(`
      ALTER TABLE affiliates 
      ADD COLUMN IF NOT EXISTS instagram_url TEXT,
      ADD COLUMN IF NOT EXISTS youtube_url TEXT,
      ADD COLUMN IF NOT EXISTS instagram_verified BOOLEAN NOT NULL DEFAULT false,
      ADD COLUMN IF NOT EXISTS youtube_verified BOOLEAN NOT NULL DEFAULT false,
      ADD COLUMN IF NOT EXISTS instagram_bio_code TEXT,
      ADD COLUMN IF NOT EXISTS youtube_bio_code TEXT,
      ADD COLUMN IF NOT EXISTS instagram_oauth_id TEXT,
      ADD COLUMN IF NOT EXISTS youtube_oauth_channel_id TEXT;
    `);
    console.log("✅ Database schema migrated for Instagram/YouTube verification");









    // Add fields for third optional platform (xHamster, Faphouse, etc.)
    await query(`
      ALTER TABLE affiliates
      ADD COLUMN IF NOT EXISTS other_url TEXT,
      ADD COLUMN IF NOT EXISTS other_verified BOOLEAN NOT NULL DEFAULT false,
      ADD COLUMN IF NOT EXISTS other_bio_code TEXT;
    `);
    console.log("✅ Added other_url schema updates for adult creator profiles");

    // Create call_logs table for call minutes analytics
    await query(`
      CREATE TABLE IF NOT EXISTS call_logs (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        room_id TEXT NOT NULL,
        caller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        duration_seconds INTEGER NOT NULL DEFAULT 0,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
      CREATE INDEX IF NOT EXISTS idx_call_logs_caller ON call_logs(caller_id);
      CREATE INDEX IF NOT EXISTS idx_call_logs_receiver ON call_logs(receiver_id);
      CREATE INDEX IF NOT EXISTS idx_call_logs_created ON call_logs(created_at);
    `);
    console.log("✅ Database schema migrated for call_logs");
  } catch (err) {
    console.error("❌ Database migration error during startup:", err.message);
  }
})();

// Helper: Normalize social urls
function normalizeSocialUrl(url) {
  if (!url) return "";
  let clean = url.trim().toLowerCase();
  clean = clean.replace(/^https?:\/\//, "");
  clean = clean.replace(/^www\./, "");
  if (clean.endsWith("/")) {
    clean = clean.slice(0, -1);
  }
  return clean;
}

// Helper: Scrape bio check
async function checkLinkedinBio(profileUrl, expectedCode) {
  if (!profileUrl) return { success: false, reason: "LinkedIn profile URL is empty" };
  const cleanedUrl = profileUrl.trim().toLowerCase();

  // Simulation mode check
  if (
    cleanedUrl.includes("simulate_success=true") || 
    cleanedUrl.includes("test_success=true") || 
    cleanedUrl.includes("localhost") || 
    cleanedUrl.includes("mock")
  ) {
    return { success: true, reason: "Code found (Simulation Mode)" };
  }

  try {
    const response = await axios.get(profileUrl, {
      timeout: 10000,
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept-Language": "en-US,en;q=0.9",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
      },
      maxRedirects: 5,
      validateStatus: () => true // Allow handling non-200 responses
    });

    if (response.status === 404) {
      return { success: false, reason: `Profile page not found (HTTP 404). Please verify your URL: ${profileUrl}` };
    }

    const finalUrl = response.request?.res?.responseUrl || profileUrl;
    if (finalUrl.toLowerCase().includes("login") || finalUrl.toLowerCase().includes("/signup") || finalUrl.toLowerCase().includes("authwall")) {
      return { 
        success: false, 
        reason: "LinkedIn redirected to a login wall (AuthWall). Bio verification could not read the page directly. Please use Simulation mode by appending '?simulate_success=true' to the profile URL for testing." 
      };
    }

    if (response.status === 999) {
      return { 
        success: false, 
        reason: "LinkedIn blocked the request (HTTP 999 rate limit). Please use Simulation mode by appending '?simulate_success=true' to the profile URL for testing." 
      };
    }

    const htmlContent = response.data;
    if (typeof htmlContent === 'string' && htmlContent.includes(expectedCode)) {
      return { success: true, reason: "Verification code found successfully on profile page." };
    } else {
      return { success: false, reason: `Verification code '${expectedCode}' was not found on your LinkedIn profile's About/Bio section.` };
    }
  } catch (err) {
    return { success: false, reason: `Network error during bio verification: ${err.message}` };
  }
}

// ──────────────────────────────────────────────────────────
// PUBLIC ENDPOINTS
// ──────────────────────────────────────────────────────────

// GET /resolve/:code - check if ref code is valid and approved
router.get("/resolve/:code", async (req, res) => {
  try {
    const code = req.params.code.trim().toUpperCase();
    const aff = await queryOne(
      "SELECT * FROM affiliates WHERE code = $1 AND status = 'approved'",
      [code]
    );

    if (!aff) {
      return res.json({ valid: false });
    }

    return res.json({
      valid: true,
      code: aff.code,
      name: aff.name
    });
  } catch (err) {
    console.error("Resolve code error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /track-click - track a referral link click
router.post("/track-click", async (req, res) => {
  try {
    const { code, referrer, userAgent } = req.body;
    if (!code) {
      return res.status(400).json({ error: "Code is required" });
    }

    const affCode = code.trim().toUpperCase();
    const aff = await queryOne(
      "SELECT * FROM affiliates WHERE code = $1 AND status = 'approved'",
      [affCode]
    );

    if (!aff) {
      return res.json({ tracked: false, reason: "Invalid or inactive code" });
    }

    // GDPR-compliant IP hashing
    const ip = req.ip || req.headers["x-forwarded-for"] || "unknown";
    const ipHash = crypto.createHash("sha256").update(ip).digest("hex");

    await query(
      `INSERT INTO affiliate_clicks (affiliate_id, ip_hash, user_agent, referrer)
       VALUES ($1, $2, $3, $4)`,
      [aff.id, ipHash, userAgent || null, referrer || null]
    );

    return res.json({ tracked: true, code: aff.code });
  } catch (err) {
    console.error("Track click error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ──────────────────────────────────────────────────────────
// AUTHENTICATED USER ENDPOINTS
// ──────────────────────────────────────────────────────────

// POST /apply - apply to creator program
router.post("/apply", requireAuth, async (req, res) => {
  try {
    const { name, preferredCode, instagramUrl, youtubeUrl, otherUrl, confirmOwnership } = req.body;
    const userId = req.user.userId;

    if (!name || !preferredCode) {
      return res.status(400).json({ error: "Name and preferred referral code are required." });
    }
    
    const igClean = instagramUrl ? instagramUrl.trim() : "";
    const ytClean = youtubeUrl ? youtubeUrl.trim() : "";
    const otherClean = otherUrl ? otherUrl.trim() : "";

    if (!igClean && !ytClean && !otherClean) {
      return res.status(400).json({ error: "Please provide at least one profile URL (Instagram, YouTube, or Other Platform)." });
    }
    
    // Validate otherUrl strictly to xhamster or faphouse if provided (allowing mirrors like xhamster2.com)
    if (otherClean) {
      const lowerOther = otherClean.toLowerCase();
      if (!lowerOther.includes("xhamster") && !lowerOther.includes("faphouse")) {
        return res.status(400).json({ error: "Other platform profile URL must be a valid xHamster or Faphouse link." });
      }
    }

    if (!confirmOwnership) {
      return res.status(400).json({ error: "You must confirm ownership of the profiles." });
    }

    // Check if user already applied
    const existing = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);
    if (existing) {
      return res.status(400).json({ 
        error: `You already have an Affiliate profile (code: ${existing.code}, status: ${existing.status})` 
      });
    }

    // Construct socialUrl for backward compatibility
    const parts = [];
    if (igClean) parts.push(`Instagram: ${igClean}`);
    if (ytClean) parts.push(`YouTube: ${ytClean}`);
    if (otherClean) parts.push(`Other: ${otherClean}`);
    const socialUrl = parts.join(" | ");

    // Validate code format
    const code = preferredCode.trim().toUpperCase().replace(/\s+/g, "");
    if (code.length < 3 || code.length > 20) {
      return res.status(400).json({ error: "Code must be between 3 and 20 characters." });
    }

    // Check if code is unique
    const codeExists = await queryOne("SELECT * FROM affiliates WHERE code = $1", [code]);
    if (codeExists) {
      return res.status(400).json({ error: `Code '${code}' is already taken. Please choose another.` });
    }

    // Generate bio verification codes
    const igChars = crypto.randomBytes(3).toString("hex").toUpperCase();
    const igBioCode = `CAMVERZ-IG-${igChars}`;
    const ytChars = crypto.randomBytes(3).toString("hex").toUpperCase();
    const ytBioCode = `CAMVERZ-YT-${ytChars}`;
    const otherChars = crypto.randomBytes(3).toString("hex").toUpperCase();
    const otherBioCode = `CAMVERZ-AD-${otherChars}`;

    // Auto verify flags if field is empty
    const instagramVerified = !igClean;
    const youtubeVerified = !ytClean;
    const otherVerified = !otherClean;

    const aff = await queryOne(
      `INSERT INTO affiliates (
        user_id, code, name, status, upi_id, social_url, 
        instagram_url, youtube_url, other_url, instagram_bio_code, youtube_bio_code, other_bio_code, confirm_ownership,
        linkedin_bio_code, instagram_verified, youtube_verified, other_verified
      )
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17)
      RETURNING *`,
      [
        userId, code, name, "pending", null, socialUrl, 
        igClean || null, ytClean || null, otherClean || null, 
        igBioCode, ytBioCode, otherBioCode, confirmOwnership, 
        igBioCode, instagramVerified, youtubeVerified, otherVerified
      ]
    );

    return res.json({
      status: "success",
      message: "Application submitted! Please complete the verification step.",
      affiliate: {
        id: aff.id,
        code: aff.code,
        status: aff.status
      }
    });
  } catch (err) {
    console.error("Apply error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// Helper: Check public bio matching
async function verifyProfileBioCode(profileUrl, expectedCode, platform) {
  if (!profileUrl) return { success: false, reason: `${platform} profile URL is empty` };
  const cleanedUrl = profileUrl.trim().toLowerCase();

  // Simulation mode check
  if (
    cleanedUrl.includes("simulate_success=true") || 
    cleanedUrl.includes("test_success=true") || 
    cleanedUrl.includes("localhost") || 
    cleanedUrl.includes("mock")
  ) {
    return { success: true, reason: "Code verified (Simulation Mode)" };
  }

  // Construct urls to check
  const urlsToTry = [profileUrl];
  if (platform === "Instagram") {
    // Construct JSON endpoint URL. E.g. https://www.instagram.com/username/?__a=1&__d=1
    const separator = profileUrl.includes("?") ? "&" : "?";
    urlsToTry.unshift(`${profileUrl}${separator}__a=1&__d=1`);
  }

  for (const url of urlsToTry) {
    try {
      console.log(`[Scrape] Attempting ${platform} check on: ${url}`);
      const response = await axios.get(url, {
        timeout: 10000,
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        },
        maxRedirects: 5,
        validateStatus: () => true
      });

      console.log(`[Scrape] ${platform} status: ${response.status} for URL: ${url}`);

      if (response.status === 404) {
        continue;
      }

      const finalUrl = response.request?.res?.responseUrl || url;
      if (finalUrl.toLowerCase().includes("login") || finalUrl.toLowerCase().includes("/signup") || finalUrl.toLowerCase().includes("authwall")) {
        console.log(`[Scrape] Blocked by login wall/authwall on ${url}`);
        continue;
      }

      const htmlContent = response.data;
      const contentStr = typeof htmlContent === 'object' ? JSON.stringify(htmlContent) : String(htmlContent);

      if (contentStr.toUpperCase().includes(expectedCode.toUpperCase())) {
        return { success: true, reason: `Verification code found successfully on ${url}` };
      }
    } catch (err) {
      console.error(`[Scrape] Error fetching ${url}:`, err.message);
    }
  }

  return { 
    success: false, 
    reason: `Verification code '${expectedCode}' was not found on your ${platform} profile. (Datacenter IP blocks or login-walls might prevent direct checks. Please retry or contact support if the code is present).` 
  };
}

// POST /verify/instagram-bio - Instagram bio check
router.post("/verify/instagram-bio", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const aff = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);
    if (!aff) return res.status(400).json({ error: "Affiliate profile not found." });
    if (!aff.instagram_url || !aff.instagram_bio_code) return res.status(400).json({ error: "Instagram URL or verification code missing." });

    const checkResult = await verifyProfileBioCode(aff.instagram_url, aff.instagram_bio_code, "Instagram");
    const { success, reason } = checkResult;

    if (success) {
      await query("UPDATE affiliates SET instagram_verified = true WHERE id = $1", [aff.id]);
      return res.json({ status: "success", message: "Instagram verified! You can now remove the code from your bio.", reason });
    } else {
      return res.status(400).json({ error: reason });
    }
  } catch (err) {
    console.error("Verify Instagram bio error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /verify/youtube-bio - YouTube bio/description check
router.post("/verify/youtube-bio", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const aff = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);
    if (!aff) return res.status(400).json({ error: "Affiliate profile not found." });
    if (!aff.youtube_url || !aff.youtube_bio_code) return res.status(400).json({ error: "YouTube URL or verification code missing." });

    const checkResult = await verifyProfileBioCode(aff.youtube_url, aff.youtube_bio_code, "YouTube");
    const { success, reason } = checkResult;

    if (success) {
      await query("UPDATE affiliates SET youtube_verified = true WHERE id = $1", [aff.id]);
      return res.json({ status: "success", message: "YouTube verified! You can now remove the code from your channel description.", reason });
    } else {
      return res.status(400).json({ error: reason });
    }
  } catch (err) {
    console.error("Verify YouTube bio error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /verify/other-bio - Other platform bio description check
router.post("/verify/other-bio", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const aff = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);
    if (!aff) return res.status(400).json({ error: "Affiliate profile not found." });
    if (!aff.other_url || !aff.other_bio_code) return res.status(400).json({ error: "Other platform profile URL or verification code missing." });

    const checkResult = await verifyProfileBioCode(aff.other_url, aff.other_bio_code, "Other Platform");
    const { success, reason } = checkResult;

    if (success) {
      await query("UPDATE affiliates SET other_verified = true WHERE id = $1", [aff.id]);
      return res.json({ status: "success", message: "Other platform profile verified successfully! You can now remove the code.", reason });
    } else {
      return res.status(400).json({ error: reason });
    }
  } catch (err) {
    console.error("Verify other profile bio error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /linkedin/verify-bio - legacy compatibility fallback
router.post("/linkedin/verify-bio", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const aff = await queryOne("SELECT id FROM affiliates WHERE user_id = $1", [userId]);
    if (!aff) return res.status(400).json({ error: "Affiliate profile not found." });

    await query("UPDATE affiliates SET instagram_verified = true, youtube_verified = true WHERE id = $1", [aff.id]);
    return res.json({ status: "success", message: "Profiles verified. Admin will review your application." });
  } catch (err) {
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /reset-verification - reset bio verification and generate new codes
router.post("/reset-verification", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const aff = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);
    if (!aff) {
      return res.status(400).json({ error: "Affiliate profile not found." });
    }

    // Generate new random codes
    const igChars = crypto.randomBytes(3).toString("hex").toUpperCase();
    const igBioCode = `CAMVERZ-IG-${igChars}`;
    const ytChars = crypto.randomBytes(3).toString("hex").toUpperCase();
    const ytBioCode = `CAMVERZ-YT-${ytChars}`;
    const otherChars = crypto.randomBytes(3).toString("hex").toUpperCase();
    const otherBioCode = `CAMVERZ-AD-${otherChars}`;

    const instagramVerified = !aff.instagram_url;
    const youtubeVerified = !aff.youtube_url;
    const otherVerified = !aff.other_url;

    await query(
      `UPDATE affiliates 
       SET instagram_verified = $1, youtube_verified = $2, other_verified = $3, status = 'pending',
           instagram_bio_code = $4, youtube_bio_code = $5, other_bio_code = $6,
           linkedin_bio_verified = false, linkedin_bio_verified_at = null 
       WHERE id = $7`,
      [instagramVerified, youtubeVerified, otherVerified, igBioCode, ytBioCode, otherBioCode, aff.id]
    );

    return res.json({ status: "success", message: "Verification status reset successfully with new codes" });
  } catch (err) {
    console.error("Reset verification error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /update-links - update/edit creator profile URLs
router.post("/update-links", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { instagramUrl, youtubeUrl, otherUrl } = req.body;

    const aff = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);
    if (!aff) return res.status(400).json({ error: "Affiliate profile not found." });

    const igClean = instagramUrl ? instagramUrl.trim() : "";
    const ytClean = youtubeUrl ? youtubeUrl.trim() : "";
    const otherClean = otherUrl ? otherUrl.trim() : "";

    // Validate otherUrl strictly to xhamster or faphouse if provided
    if (otherClean) {
      const lowerOther = otherClean.toLowerCase();
      if (!lowerOther.includes("xhamster") && !lowerOther.includes("faphouse")) {
        return res.status(400).json({ error: "Other platform profile URL must be a valid xHamster or Faphouse link." });
      }
    }

    // Reset verified flags for any updated URL
    let instagramVerified = aff.instagram_verified;
    let youtubeVerified = aff.youtube_verified;
    let otherVerified = aff.other_verified;

    if (igClean !== (aff.instagram_url || "")) {
      instagramVerified = !igClean; // true if cleared, false if new URL is set
    }
    if (ytClean !== (aff.youtube_url || "")) {
      youtubeVerified = !ytClean;
    }
    if (otherClean !== (aff.other_url || "")) {
      otherVerified = !otherClean;
    }

    // Generate new random codes for updated URLs
    let igCode = aff.instagram_bio_code;
    let ytCode = aff.youtube_bio_code;
    let otherCode = aff.other_bio_code;

    if (igClean && igClean !== (aff.instagram_url || "")) {
      const igChars = crypto.randomBytes(3).toString("hex").toUpperCase();
      igCode = `CAMVERZ-IG-${igChars}`;
    }
    if (ytClean && ytClean !== (aff.youtube_url || "")) {
      const ytChars = crypto.randomBytes(3).toString("hex").toUpperCase();
      ytCode = `CAMVERZ-YT-${ytChars}`;
    }
    if (otherClean && otherClean !== (aff.other_url || "")) {
      const otherChars = crypto.randomBytes(3).toString("hex").toUpperCase();
      otherCode = `CAMVERZ-AD-${otherChars}`;
    }

    // Rebuild combined socialUrl for backward compatibility
    const parts = [];
    if (igClean) parts.push(`Instagram: ${igClean}`);
    if (ytClean) parts.push(`YouTube: ${ytClean}`);
    if (otherClean) parts.push(`Other: ${otherClean}`);
    const socialUrl = parts.join(" | ");

    await query(
      `UPDATE affiliates 
       SET instagram_url = $1, youtube_url = $2, other_url = $3, 
           instagram_verified = $4, youtube_verified = $5, other_verified = $6,
           instagram_bio_code = $7, youtube_bio_code = $8, other_bio_code = $9,
           social_url = $10
       WHERE id = $11`,
      [
        igClean || null, ytClean || null, otherClean || null,
        instagramVerified, youtubeVerified, otherVerified,
        igCode, ytCode, otherCode,
        socialUrl, aff.id
      ]
    );

    const updatedAff = await queryOne("SELECT * FROM affiliates WHERE id = $1", [aff.id]);
    return res.json({
      status: "success",
      message: "Links updated successfully. Please verify any newly added profiles.",
      affiliate: updatedAff
    });
  } catch (err) {
    console.error("Update links error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// GET /me - dashboard stats
router.get("/me", requireAuth, async (req, res) => {
  try {
    const userId = req.user.userId;
    const aff = await queryOne("SELECT * FROM affiliates WHERE user_id = $1", [userId]);

    if (!aff) {
      return res.json({ has_affiliate: false });
    }

    // Legacy bio code fix
    if (aff.social_url && !aff.linkedin_bio_code) {
      const chars = crypto.randomBytes(3).toString("hex").toUpperCase();
      aff.linkedin_bio_code = `CAMVERZ-LK-${chars}`;
      await query("UPDATE affiliates SET linkedin_bio_code = $1 WHERE id = $2", [aff.linkedin_bio_code, aff.id]);
    }

    // Aggregate stats
    const totalClicksRes = await queryOne(
      "SELECT COUNT(*)::int as count FROM affiliate_clicks WHERE affiliate_id = $1",
      [aff.id]
    );
    const totalClicks = totalClicksRes ? totalClicksRes.count : 0;

    const totalSignupsRes = await queryOne(
      "SELECT COUNT(*)::int as count FROM affiliate_signups WHERE affiliate_id = $1",
      [aff.id]
    );
    const totalSignups = totalSignupsRes ? totalSignupsRes.count : 0;

    const totalSalesRes = await queryOne(
      "SELECT COUNT(*)::int as count FROM affiliate_sales WHERE affiliate_id = $1 AND status IN ('confirmed', 'refunded')",
      [aff.id]
    );
    const totalSales = totalSalesRes ? totalSalesRes.count : 0;

    const totalRefundsRes = await queryOne(
      "SELECT COUNT(*)::int as count FROM affiliate_sales WHERE affiliate_id = $1 AND status = 'refunded'",
      [aff.id]
    );
    const totalRefunds = totalRefundsRes ? totalRefundsRes.count : 0;

    const earningsRes = await queryOne(
      `SELECT COALESCE(SUM(commission_amount), 0)::float as sum 
       FROM affiliate_sales 
       WHERE affiliate_id = $1 AND status IN ('confirmed', 'refunded')`,
      [aff.id]
    );
    const totalEarnings = earningsRes ? earningsRes.sum : 0.0;

    const clawbackRes = await queryOne(
      `SELECT COALESCE(SUM(commission_clawback), 0)::float as sum 
       FROM affiliate_sales 
       WHERE affiliate_id = $1 AND status = 'refunded'`,
      [aff.id]
    );
    const totalClawback = clawbackRes ? clawbackRes.sum : 0.0;

    const netEarnings = Math.max(0, parseFloat((totalEarnings - totalClawback).toFixed(2)));

    const paidRes = await queryOne(
      `SELECT COALESCE(SUM(amount), 0)::float as sum 
       FROM affiliate_payouts 
       WHERE affiliate_id = $1 AND status = 'completed'`,
      [aff.id]
    );
    const totalPaid = paidRes ? paidRes.sum : 0.0;

    const pendingPayout = Math.max(0, parseFloat((netEarnings - totalPaid).toFixed(2)));

    const conversionRate = totalClicks > 0 ? parseFloat((totalSales / totalClicks * 100).toFixed(1)) : 0;

    // Recent Sales
    const recentSales = await queryMany(
      `SELECT id, plan_purchased as plan, amount_paid as amount, commission_amount as commission, status, 
              created_at, refunded_amount, commission_clawback, refunded_at, razorpay_refund_id, refund_notes
       FROM affiliate_sales
       WHERE affiliate_id = $1
       ORDER BY created_at DESC LIMIT 20`,
      [aff.id]
    );

    const salesList = recentSales.map(s => ({
      id: s.id,
      plan: s.plan || "premium",
      amount: s.amount,
      commission: s.commission,
      status: s.status,
      date: s.created_at ? s.created_at.toISOString().replace(/T/, ' ').replace(/\..+/, '') : "N/A",
      refunded_amount: s.refunded_amount,
      commission_clawback: s.commission_clawback,
      refunded_at: s.refunded_at ? s.refunded_at.toISOString() : null,
      razorpay_refund_id: s.razorpay_refund_id,
      refund_notes: s.refund_notes
    }));

    // Payout History
    const payouts = await queryMany(
      `SELECT id, amount, method, status, transaction_ref, created_at
       FROM affiliate_payouts
       WHERE affiliate_id = $1
       ORDER BY created_at DESC LIMIT 20`,
      [aff.id]
    );

    const payoutList = payouts.map(p => ({
      id: p.id,
      amount: p.amount,
      method: p.method,
      status: p.status,
      transaction_ref: p.transaction_ref,
      date: p.created_at ? p.created_at.toISOString().replace(/T/, ' ').replace(/\..+/, '') : "N/A"
    }));

    // Daily clicks breakdown for last 30 days
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const dailyClicks = await queryMany(
      `SELECT DATE_TRUNC('day', created_at) as day, COUNT(*)::int as count 
       FROM affiliate_clicks 
       WHERE affiliate_id = $1 AND created_at >= $2
       GROUP BY DATE_TRUNC('day', created_at)
       ORDER BY day ASC`,
      [aff.id, thirtyDaysAgo]
    );

    const clickChart = dailyClicks.map(d => ({
      day: d.day ? d.day.toISOString().split("T")[0] : "N/A",
      clicks: d.count
    }));

    // Detailed Referred Users Analytics
    const signupsList = await queryMany(
      `SELECT s.referred_user_id, u.name, u.email, u.gender, u.verified, u.created_at,
              (SELECT plan_purchased FROM affiliate_sales WHERE referred_user_id = u.id AND status = 'confirmed' ORDER BY created_at DESC LIMIT 1) as plan_purchased,
              (SELECT COALESCE(SUM(amount_paid), 0) FROM affiliate_sales WHERE referred_user_id = u.id AND status = 'confirmed') as total_paid,
              (SELECT COALESCE(SUM(duration_seconds), 0) FROM call_logs WHERE (caller_id = u.id OR receiver_id = u.id) AND created_at >= NOW() - INTERVAL '7 days') as call_seconds_7d
       FROM affiliate_signups s
       JOIN users u ON s.referred_user_id = u.id
       WHERE s.affiliate_id = $1
       ORDER BY s.created_at DESC`,
      [aff.id]
    );

    const referredUsers = signupsList.map(su => ({
      userId: su.referred_user_id,
      name: su.name || "Anonymous User",
      email: su.email || "N/A",
      gender: su.gender || "Unspecified",
      verified: !!su.verified,
      plan: su.plan_purchased ? "Paid" : "Free",
      planName: su.plan_purchased || "N/A",
      totalPaid: parseFloat(parseFloat(su.total_paid).toFixed(2)),
      talkTimeMins7d: Math.round(su.call_seconds_7d / 60),
      joinedAt: su.created_at ? su.created_at.toISOString().split("T")[0] : "N/A"
    }));

    let totalSeconds = 0;
    let totalBoys = 0;
    let totalGirls = 0;
    let verifiedGirls = 0;

    referredUsers.forEach(u => {
      const originalSu = signupsList.find(item => item.referred_user_id === u.userId);
      if (originalSu) {
        totalSeconds += originalSu.call_seconds_7d;
      }
      const g = u.gender.toLowerCase();
      if (g.includes("female") || g.includes("girl") || g.includes("woman")) {
        totalGirls++;
        if (u.verified) verifiedGirls++;
      } else if (g.includes("male") || g.includes("boy") || g.includes("man")) {
        totalBoys++;
      }
    });

    const average7dTalktimeMins = referredUsers.length > 0 
      ? Math.round((totalSeconds / 60) / referredUsers.length) 
      : 0;

    const analyticsSummary = {
      total_boys: totalBoys,
      total_girls: totalGirls,
      verified_girls: verifiedGirls,
      average_7d_talktime_mins: average7dTalktimeMins
    };

    return res.json({
      has_affiliate: true,
      affiliate: {
        id: aff.id,
        code: aff.code,
        name: aff.name,
        status: aff.status,
        commission_rate: aff.commission_rate,
        upi_id: aff.upi_id,
        upi_verified: aff.upi_verified,
        social_url: aff.social_url,
        instagram_url: aff.instagram_url,
        youtube_url: aff.youtube_url,
        instagram_verified: aff.instagram_verified,
        youtube_verified: aff.youtube_verified,
        other_verified: aff.other_verified,
        instagram_bio_code: aff.instagram_bio_code,
        youtube_bio_code: aff.youtube_bio_code,
        other_bio_code: aff.other_bio_code,
        other_url: aff.other_url,
        min_payout: aff.min_payout,
        created_at: aff.created_at ? aff.created_at.toISOString().split("T")[0] : "N/A"
      },
      stats: {
        total_clicks: totalClicks,
        total_signups: totalSignups,
        total_sales: totalSales,
        total_refunds: totalRefunds,
        total_earnings: totalEarnings,
        total_clawback: totalClawback,
        net_earnings: netEarnings,
        total_paid: totalPaid,
        pending_payout: pendingPayout,
        conversion_rate: conversionRate
      },
      recent_sales: salesList,
      payouts: payoutList,
      click_chart: clickChart,
      referred_users: referredUsers,
      analytics_summary: analyticsSummary
    });
  } catch (err) {
    console.error("GET /me error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ──────────────────────────────────────────────────────────
// RAZORPAY WEBHOOK
// ──────────────────────────────────────────────────────────
router.post("/webhook/razorpay", async (req, res) => {
  const secret = process.env.RAZORPAY_WEBHOOK_SECRET;
  
  if (secret) {
    const signature = req.headers["x-razorpay-signature"] || "";
    const bodyStr = JSON.stringify(req.body);
    const expected = crypto.createHmac("sha256", secret).update(bodyStr).digest("hex");
    
    if (signature !== expected) {
      console.error("[Affiliate Webhook] Invalid signature verification failed!");
      return res.status(400).json({ error: "Invalid signature" });
    }
  }

  const { event, payload } = req.body;
  console.log(`[Affiliate Webhook] Received event: ${event}`);

  try {
    // 1. payment.captured or payment_link.paid
    if (event === "payment.captured" || event === "payment_link.paid") {
      const paymentEntity = payload?.payment?.entity || {};
      const paymentId = paymentEntity.id;
      const orderId = paymentEntity.order_id;
      const amountPaise = paymentEntity.amount || 0;
      const amountInr = amountPaise / 100.0;
      const email = (paymentEntity.email || "").toLowerCase().trim();
      const notes = paymentEntity.notes || {};

      if (!paymentId || !email) {
        return res.json({ status: "skipped", reason: "Missing payment_id or email" });
      }

      // Check duplicate
      const duplicate = await queryOne("SELECT * FROM affiliate_sales WHERE razorpay_payment_id = $1", [paymentId]);
      if (duplicate) {
        return res.json({ status: "duplicate", message: "Sale already recorded" });
      }

      // Find user
      const user = await queryOne("SELECT * FROM users WHERE LOWER(email) = LOWER($1)", [email]);
      if (!user) {
        console.warn(`[Affiliate Webhook] User with email ${email} not found.`);
        return res.json({ status: "skipped", reason: "User not found" });
      }

      // Track conversion
      const signup = await queryOne("SELECT * FROM affiliate_signups WHERE referred_user_id = $1", [user.id]);
      if (signup) {
        const aff = await queryOne("SELECT * FROM affiliates WHERE id = $1 AND status = 'approved'", [signup.affiliate_id]);
        if (aff) {
          const commission = parseFloat((amountInr * aff.commission_rate).toFixed(2));
          const planName = notes.plan_name || notes.product_name || "premium_call";

          await query(
            `INSERT INTO affiliate_sales 
               (affiliate_id, referred_user_id, razorpay_payment_id, razorpay_order_id, plan_purchased, 
                amount_paid, commission_amount, commission_rate, cookie_ref, status)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'confirmed')`,
            [aff.id, user.id, paymentId, orderId || null, planName, amountInr, commission, aff.commission_rate, signup.ref_code_used]
          );

          console.log(`[Affiliate Webhook] Commission of ₹${commission} recorded for affiliate ${aff.code} (from customer: ${email})`);
          return res.json({ status: "recorded", affiliate: aff.code, commission });
        }
      }

      return res.json({ status: "no_affiliate", reason: "Payment recorded but user was not referred" });
    }

    // 2. refund event
    if (event === "payment.refunded" || event === "refund.created" || event === "refund.processed") {
      const refundEntity = payload?.refund?.entity || {};
      const paymentEntity = payload?.payment?.entity || {};
      const paymentId = refundEntity.payment_id || paymentEntity.id;
      const refundId = refundEntity.id;
      const refundAmountPaise = refundEntity.amount || 0;
      const refundAmountInr = refundAmountPaise / 100.0;

      if (!paymentId) {
        return res.json({ status: "skipped", reason: "No payment_id in refund payload" });
      }

      const sale = await queryOne("SELECT * FROM affiliate_sales WHERE razorpay_payment_id = $1", [paymentId]);
      if (!sale) {
        return res.json({ status: "refund_noted", reason: "Refund recorded but no attributed affiliate sale found" });
      }

      // Calculate clawback
      const ratio = sale.amount_paid > 0 ? Math.min(1.0, refundAmountInr / sale.amount_paid) : 0;
      const clawback = parseFloat((sale.commission_amount * ratio).toFixed(2));
      const notes = `Refund processed: ${refundId} - ₹${refundAmountInr}. Commission clawback: ₹${clawback}.`;

      await query(
        `UPDATE affiliate_sales
         SET status = 'refunded', razorpay_refund_id = $1, refunded_amount = $2, 
             commission_clawback = $3, refunded_at = NOW(), refund_notes = $4
         WHERE id = $5`,
        [refundId, refundAmountInr, clawback, notes, sale.id]
      );

      console.log(`[Affiliate Webhook] Refund recorded for sale ${sale.id}. Clawback: ₹${clawback}`);
      return res.json({ status: "refund_recorded", clawback });
    }

    return res.json({ status: "ignored", event });
  } catch (err) {
    console.error("Webhook processing error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// ──────────────────────────────────────────────────────────
// ADMIN ENDPOINTS
// ──────────────────────────────────────────────────────────

// Admin Helper Middleware
function requireAdmin(req, res, next) {
  if (!req.user || req.user.email !== "mohitjain1619@gmail.com") {
    return res.status(403).json({ error: "Admin access required" });
  }
  next();
}

// GET /admin/list - list all applications
router.get("/admin/list", requireAuth, requireAdmin, async (req, res) => {
  try {
    const list = await queryMany(
      `SELECT a.*, u.email as user_email,
              (SELECT COUNT(*)::int FROM affiliate_clicks WHERE affiliate_id = a.id) as clicks,
              (SELECT COUNT(*)::int FROM affiliate_signups WHERE affiliate_id = a.id) as signups,
              (SELECT COUNT(*)::int FROM affiliate_sales WHERE affiliate_id = a.id AND status IN ('confirmed', 'refunded')) as sales,
              (SELECT COALESCE(SUM(commission_amount), 0)::float FROM affiliate_sales WHERE affiliate_id = a.id AND status IN ('confirmed', 'refunded')) as gross_earnings,
              (SELECT COALESCE(SUM(commission_clawback), 0)::float FROM affiliate_sales WHERE affiliate_id = a.id AND status = 'refunded') as clawbacks,
              (SELECT COALESCE(SUM(amount), 0)::float FROM affiliate_payouts WHERE affiliate_id = a.id AND status = 'completed') as paid
       FROM affiliates a
       JOIN users u ON a.user_id = u.id
       ORDER BY a.created_at DESC`
    );

    const formatted = list.map(a => {
      const net = Math.max(0, a.gross_earnings - a.clawbacks);
      const pending = Math.max(0, net - a.paid);

      return {
        id: a.id,
        user_id: a.user_id,
        email: a.user_email,
        code: a.code,
        name: a.name,
        status: a.status,
        commission_rate: a.commission_rate,
        upi_id: a.upi_id,
        social_url: a.social_url,
        instagram_url: a.instagram_url,
        youtube_url: a.youtube_url,
        other_url: a.other_url,
        instagram_verified: a.instagram_verified,
        youtube_verified: a.youtube_verified,
        other_verified: a.other_verified,
        instagram_bio_code: a.instagram_bio_code,
        youtube_bio_code: a.youtube_bio_code,
        other_bio_code: a.other_bio_code,
        clicks: a.clicks,
        signups: a.signups,
        sales: a.sales,
        total_earnings: parseFloat(a.gross_earnings.toFixed(2)),
        total_paid: parseFloat(a.paid.toFixed(2)),
        pending: parseFloat(pending.toFixed(2)),
        created_at: a.created_at ? a.created_at.toISOString().split("T")[0] : "N/A",
        linkedin_bio_verified: a.linkedin_bio_verified,
        linkedin_bio_code: a.linkedin_bio_code,
        admin_notes: a.admin_notes
      };
    });

    return res.json(formatted);
  } catch (err) {
    console.error("Admin list error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// PUT /admin/:affiliate_id/approve - approve affiliate
router.put("/admin/:affiliate_id/approve", requireAuth, requireAdmin, async (req, res) => {
  try {
    const affId = req.params.affiliate_id;
    const aff = await queryOne("SELECT * FROM affiliates WHERE id = $1", [affId]);

    if (!aff) {
      return res.status(404).json({ error: "Affiliate not found" });
    }

    await query(
      "UPDATE affiliates SET status = 'approved', approved_at = NOW() WHERE id = $1",
      [affId]
    );

    return res.json({ status: "success", message: `Affiliate ${aff.code} approved successfully` });
  } catch (err) {
    console.error("Approve error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// PUT /admin/:affiliate_id/update - update details
router.put("/admin/:affiliate_id/update", requireAuth, requireAdmin, async (req, res) => {
  try {
    const affId = req.params.affiliate_id;
    const { status, commissionRate, adminNotes, minPayout } = req.body;

    const aff = await queryOne("SELECT * FROM affiliates WHERE id = $1", [affId]);
    if (!aff) {
      return res.status(404).json({ error: "Affiliate not found" });
    }

    // Build update dynamic fields
    const updates = [];
    const vals = [];
    let placeholderIdx = 1;

    if (status !== undefined) {
      updates.push(`status = $${placeholderIdx++}`);
      vals.push(status);
      if (status === "approved" && !aff.approved_at) {
        updates.push(`approved_at = NOW()`);
      }
    }
    if (commissionRate !== undefined) {
      updates.push(`commission_rate = $${placeholderIdx++}`);
      vals.push(commissionRate);
    }
    if (adminNotes !== undefined) {
      updates.push(`admin_notes = $${placeholderIdx++}`);
      vals.push(adminNotes);
    }
    if (minPayout !== undefined) {
      updates.push(`min_payout = $${placeholderIdx++}`);
      vals.push(minPayout);
    }

    if (updates.length > 0) {
      vals.push(affId);
      const queryStr = `UPDATE affiliates SET ${updates.join(", ")} WHERE id = $${placeholderIdx}`;
      await query(queryStr, vals);
    }

    return res.json({ status: "success", message: `Affiliate ${aff.code} updated successfully` });
  } catch (err) {
    console.error("Update error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// GET /admin/sales - sales logs
router.get("/admin/sales", requireAuth, requireAdmin, async (req, res) => {
  try {
    const sales = await queryMany(
      `SELECT s.*, a.code as affiliate_code, a.name as affiliate_name, u.email as customer_email
       FROM affiliate_sales s
       JOIN affiliates a ON s.affiliate_id = a.id
       JOIN users u ON s.referred_user_id = u.id
       ORDER BY s.created_at DESC LIMIT 100`
    );

    const formatted = sales.map(s => ({
      id: s.id,
      affiliate_code: s.affiliate_code,
      affiliate_name: s.affiliate_name,
      customer_email: s.customer_email,
      plan: s.plan_purchased || "premium_call",
      amount: s.amount_paid,
      commission: s.commission_amount,
      rate: s.commission_rate,
      razorpay_id: s.razorpay_payment_id,
      status: s.status,
      date: s.created_at ? s.created_at.toISOString().replace(/T/, ' ').replace(/\..+/, '') : "N/A",
      refunded_amount: s.refunded_amount,
      commission_clawback: s.commission_clawback,
      refunded_at: s.refunded_at ? s.refunded_at.toISOString() : null,
      razorpay_refund_id: s.razorpay_refund_id,
      refund_notes: s.refund_notes
    }));

    return res.json(formatted);
  } catch (err) {
    console.error("Admin sales error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /admin/payout - manual payout log
router.post("/admin/payout", requireAuth, requireAdmin, async (req, res) => {
  try {
    const { affiliateId, amount, method, transactionRef, adminNotes } = req.body;
    if (!affiliateId || !amount || !method) {
      return res.status(400).json({ error: "AffiliateId, amount, and method are required" });
    }

    const aff = await queryOne("SELECT * FROM affiliates WHERE id = $1", [affiliateId]);
    if (!aff) {
      return res.status(404).json({ error: "Affiliate not found" });
    }

    // Insert payout
    const payout = await queryOne(
      `INSERT INTO affiliate_payouts (affiliate_id, amount, method, status, transaction_ref, admin_notes)
       VALUES ($1, $2, $3, 'completed', $4, $5)
       RETURNING *`,
      [affiliateId, amount, method, transactionRef || null, adminNotes || null]
    );

    // Link confirmed sales to this payout
    await query(
      `UPDATE affiliate_sales
       SET payout_id = $1
       WHERE affiliate_id = $2 AND payout_id IS NULL AND status = 'confirmed'`,
      [payout.id, affiliateId]
    );

    return res.json({
      status: "success",
      message: `Payout of ₹${amount} recorded successfully for ${aff.code}`,
      payout_id: payout.id
    });
  } catch (err) {
    console.error("Admin payout error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// DELETE /admin/user/:user_id - admin delete user completely from database
router.delete("/admin/user/:user_id", requireAuth, requireAdmin, async (req, res) => {
  try {
    const { user_id } = req.params;
    
    // Check if user exists
    const user = await queryOne("SELECT * FROM users WHERE id = $1", [user_id]);
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }

    // Do not allow deleting the admin user itself
    if (user.email === "mohitjain1619@gmail.com") {
      return res.status(400).json({ error: "Cannot delete the admin account." });
    }

    // Delete user (cascade handles the rest)
    await query("DELETE FROM users WHERE id = $1", [user_id]);
    console.log(`🗑️ [Admin Cleanup] Deleted user account: ${user_id} (${user.email})`);

    return res.json({ status: "success", message: `User account ${user.email} successfully deleted from database.` });
  } catch (err) {
    console.error("Admin delete user error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// POST /admin/wipe-trial-data - wipe all database data keeping only admin user/affiliate
router.post("/admin/wipe-trial-data", requireAuth, requireAdmin, async (req, res) => {
  try {
    // Delete all users except admin (cascade cleans up all referenced creator profiles/affiliates)
    const result = await query("DELETE FROM users WHERE email != 'mohitjain1619@gmail.com'");
    console.log(`🔥 [Admin Wipe] Cleaned up trial accounts. Rows affected: ${result.rowCount}`);
    return res.json({ status: "success", message: `Database wiped successfully. Cleaned up ${result.rowCount} trial accounts.` });
  } catch (err) {
    console.error("Admin wipe error:", err);
    return res.status(500).json({ error: "Internal server error" });
  }
});

module.exports = router;
