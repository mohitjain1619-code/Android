const express = require("express");
const router = express.Router();
const crypto = require("crypto");
const axios = require("axios");
const { query } = require("../config/database");

// Ensure rewarded_ad_logs table exists
query(`
  CREATE TABLE IF NOT EXISTS rewarded_ad_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ad_unit TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  )
`).catch(err => console.error("Error creating rewarded_ad_logs table:", err));

let cachedKeys = null;
let keysExpiryTime = 0;

async function getAdMobKeys() {
  const currentTime = Date.now();
  if (cachedKeys && currentTime < keysExpiryTime) {
    return cachedKeys;
  }

  try {
    const response = await axios.get("https://www.gstatic.com/admob/reward/verifier-keys.json");
    if (response.data && response.data.keys) {
      cachedKeys = response.data.keys;
      keysExpiryTime = currentTime + 24 * 60 * 60 * 1000; // Cache 24h
      return cachedKeys;
    }
  } catch (err) {
    console.error("❌ Failed to fetch AdMob verifier keys:", err.message);
  }
  return cachedKeys || [];
}

function base64urlToBase64(base64url) {
  let base64 = base64url.replace(/-/g, "+").replace(/_/g, "/");
  while (base64.length % 4) {
    base64 += "=";
  }
  return base64;
}

// SSV Callback verify endpoint
router.get("/verify-reward", async (req, res) => {
  const queryParams = { ...req.query };
  const signature = queryParams.signature;
  const keyId = queryParams.key_id;

  if (!signature || !keyId) {
    console.error("❌ AdMob SSV: Missing signature or key_id");
    return res.status(400).send("Missing signature or key_id");
  }

  // 1. Fetch verifier keys from Google
  const keys = await getAdMobKeys();
  const matchedKey = keys.find(k => String(k.keyId) === String(keyId));

  if (!matchedKey) {
    console.error(`❌ AdMob SSV: Key not found for keyId ${keyId}`);
    return res.status(400).send("Invalid key_id");
  }

  // 2. Reconstruct original query string alphabetically
  delete queryParams.signature;
  delete queryParams.key_id;

  const sortedKeys = Object.keys(queryParams).sort();
  const message = sortedKeys
    .map(key => `${key}=${queryParams[key]}`)
    .join("&");

  // 3. Verify signature using ECDSA SHA256
  try {
    const signatureBuffer = Buffer.from(base64urlToBase64(signature), "base64");
    const verifier = crypto.createVerify("SHA256");
    verifier.update(message);

    const isValid = verifier.verify(matchedKey.pem, signatureBuffer);

    if (!isValid) {
      console.error("❌ AdMob SSV: Invalid signature");
      return res.status(400).send("Invalid signature");
    }

    // 4. Reward User
    // Google passes custom_data or user_id containing user's UUID
    const userId = queryParams.custom_data || queryParams.user_id;
    const adUnit = queryParams.ad_unit || "unknown";

    if (!userId) {
      console.error("❌ AdMob SSV: Missing user identifier (custom_data/user_id)");
      return res.status(400).send("Missing user identifier");
    }

    // Validate if user exists in the database
    const userResult = await query("SELECT id FROM users WHERE id = $1", [userId]);
    if (!userResult.rows || userResult.rows.length === 0) {
      console.error(`❌ AdMob SSV: User ${userId} not found in database`);
      return res.status(400).send("User not found");
    }

    // Record the rewarded ad watch
    await query(
      "INSERT INTO rewarded_ad_logs (user_id, ad_unit) VALUES ($1, $2)",
      [userId, adUnit]
    );

    console.log(`🎉 [AdMob Reward Credited] User: ${userId}, AdUnit: ${adUnit}`);
    return res.status(200).send("OK");

  } catch (err) {
    console.error("❌ AdMob SSV error:", err.message);
    return res.status(500).send("Internal verification error");
  }
});

module.exports = router;
