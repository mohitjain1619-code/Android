const express = require("express");
const multer = require("multer");
const { queryOne } = require("../config/database");
const { requireAuth } = require("../middleware/auth");
const { detectGenderFromImage } = require("../../services/rekognition");

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage() });

router.use(requireAuth);

// ============================================
// POST /verify/gender — Gender verification via AWS Rekognition
// Same logic as before, but uses JWT auth instead of Firebase token
// ============================================
router.post("/gender", upload.single("image"), async (req, res) => {
  try {
    const user = await queryOne("SELECT * FROM users WHERE id = $1", [req.user.userId]);
    if (!user) {
      return res.status(404).json({ error: "User profile not found" });
    }

    const profileGender = (user.gender || "").trim().toLowerCase();
    if (!profileGender) {
      return res.status(400).json({ error: "Profile gender is missing" });
    }

    // Males auto-verify
    if (profileGender === "male") {
      return res.json({
        ok: true,
        verified: true,
        policy: "male_auto_verified",
        profileGender,
      });
    }

    // Females require Rekognition verification
    if (!req.file || !req.file.buffer) {
      return res.status(400).json({ error: "Image file is required (field name: image)" });
    }

    const result = await detectGenderFromImage(req.file.buffer);
    if (result.error) {
      return res.status(400).json({ ok: false, verified: false, error: result.error });
    }

    const detectedGender = (result.gender || "").trim().toLowerCase();
    const confidence = Number(result.confidence || 0);
    const minConfidence = Number(process.env.REKOGNITION_MIN_CONFIDENCE || 80);

    const verified = profileGender === "female"
      ? (detectedGender === "female" && confidence >= minConfidence)
      : false;

    let genderChangedToMale = false;

    // Update verification status in database
    if (verified) {
      await queryOne("UPDATE users SET verified = true WHERE id = $1 RETURNING *", [req.user.userId]);
    } else if (profileGender === "female" && detectedGender === "male" && confidence >= minConfidence) {
      // Detected boy during female face scan: automatically switch profile gender to male & auto-verify
      await queryOne("UPDATE users SET gender = 'male', verified = true WHERE id = $1 RETURNING *", [req.user.userId]);
      genderChangedToMale = true;
      console.log(`[Verification] User ${req.user.userId} selected female but was detected as male (${confidence}%). Auto-updated gender to male and verified.`);
    }

    return res.json({
      ok: true,
      verified,
      genderChangedToMale,
      policy: "female_strict_verification",
      profileGender: genderChangedToMale ? "male" : profileGender,
      detectedGender,
      confidence,
      minConfidence,
    });
  } catch (err) {
    console.error("Gender verification error:", err);
    return res.status(500).json({ error: "Internal error", details: err.message });
  }
});

module.exports = router;
