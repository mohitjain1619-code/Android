const { initializeApp, cert, applicationDefault, getApps } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const path = require("path");
const fs = require("fs");
const { query, queryOne, queryMany } = require("../config/database");

let isInitialized = false;

// Initialize Firebase Admin SDK
function initFirebase() {
  if (isInitialized || getApps().length > 0) {
    isInitialized = true;
    return true;
  }

  try {
    const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH ||
      path.join(__dirname, "../../config/firebase-service-account.json");

    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const credentials = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
      initializeApp({
        credential: cert(credentials),
      });
      isInitialized = true;
      console.log("✅ Firebase Admin SDK Initialized via FIREBASE_SERVICE_ACCOUNT_JSON");
      return true;
    } else if (fs.existsSync(serviceAccountPath)) {
      const serviceAccount = require(serviceAccountPath);
      initializeApp({
        credential: cert(serviceAccount),
      });
      isInitialized = true;
      console.log("✅ Firebase Admin SDK Initialized via file: " + serviceAccountPath);
      return true;
    } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS && fs.existsSync(process.env.GOOGLE_APPLICATION_CREDENTIALS)) {
      initializeApp({
        credential: applicationDefault(),
      });
      isInitialized = true;
      console.log("✅ Firebase Admin SDK Initialized via GOOGLE_APPLICATION_CREDENTIALS");
      return true;
    } else {
      console.warn("⚠️ Firebase service account key not found. Push notifications will be queued/logged until key is provided.");
      return false;
    }
  } catch (err) {
    console.error("❌ Error initializing Firebase Admin SDK:", err.message);
    return false;
  }
}

// Auto-initialize on module load
initFirebase();

/**
 * Production-grade Push Notification Dispatcher for 10 Lakh users scale.
 * 
 * Channels:
 * - camverz_calls: Urgent priority for video/voice calls (Ringing, heads-up)
 * - camverz_chats: High priority for private chat messages
 * - camverz_social: Standard priority for likes, comments, RealMeet
 * - camverz_marketing: Standard priority for broadcast offers/announcements
 */
class NotificationService {

  /**
   * Send push notification to a single user by User ID
   * Non-blocking, handles dead token cleanup automatically.
   */
  static sendToUser(recipientUserId, { title, body, type = "general", data = {}, channelId = "camverz_social", sound = "default", highPriority = false }) {
    if (!recipientUserId) return;

    // Execute asynchronously so the calling route responds immediately without delay
    setImmediate(async () => {
      try {
        const user = await queryOne(
          "SELECT fcm_token FROM users WHERE id = $1 AND fcm_token IS NOT NULL",
          [recipientUserId]
        );

        if (!user || !user.fcm_token) {
          return; // User has no token registered or offline
        }

        const token = user.fcm_token;
        await NotificationService.sendToToken(token, recipientUserId, {
          title,
          body,
          type,
          data,
          channelId,
          sound,
          highPriority
        });
      } catch (err) {
        console.error(`[NotificationService] Error sending push to user ${recipientUserId}:`, err.message);
      }
    });
  }

  /**
   * Internal helper to send message to raw FCM token with dead token removal
   */
  static async sendToToken(token, userId, { title, body, type = "general", data = {}, channelId = "camverz_social", sound = "default", highPriority = false }) {
    if (!initFirebase()) {
      console.log(`[FCM Mock] Push for user ${userId} [${type}]: ${title} - ${body}`);
      return;
    }

    try {
      // Stringify all data payload properties for FCM compliance
      const stringifiedData = {
        type: String(type),
        title: String(title || ""),
        body: String(body || ""),
        channelId: String(channelId),
        timestamp: String(Date.now()),
      };

      for (const [key, value] of Object.entries(data)) {
        if (value !== undefined && value !== null) {
          stringifiedData[key] = typeof value === "object" ? JSON.stringify(value) : String(value);
        }
      }

      const message = {
        token: token,
        notification: {
          title: title,
          body: body,
        },
        data: stringifiedData,
        android: {
          priority: highPriority ? "high" : "normal",
          notification: {
            channelId: channelId,
            sound: sound,
            defaultSound: sound === "default",
            defaultVibrateTimings: true,
            icon: "ic_notification",
            color: "#6366F1",
            clickAction: "OPEN_CAMVERZ_NOTIFICATION",
          },
        },
      };

      const response = await getMessaging().send(message);
      console.log(`[FCM Success] Sent [${type}] to user ${userId} (ID: ${response})`);
    } catch (err) {
      console.error(`[FCM Error] Failed sending to user ${userId}:`, err.code || err.message);

      // Dead token cleanup: if app was uninstalled or token expired, delete from DB
      if (
        err.code === "messaging/registration-token-not-registered" ||
        err.code === "messaging/invalid-registration-token" ||
        err.code === "messaging/invalid-argument"
      ) {
        console.warn(`[FCM Cleanup] Removing stale token for user ${userId}`);
        try {
          await query("UPDATE users SET fcm_token = NULL, fcm_updated_at = NOW() WHERE id = $1", [userId]);
        } catch (dbErr) {
          console.error("Failed to clean dead token:", dbErr.message);
        }
      }
    }
  }

  /**
   * Send notification to a list of users (Batched in chunks of 500 for large-scale safety)
   */
  static sendToMultipleUsers(userIds, { title, body, type = "general", data = {}, channelId = "camverz_social" }) {
    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) return;

    setImmediate(async () => {
      try {
        const rows = await queryMany(
          "SELECT id, fcm_token FROM users WHERE id = ANY($1) AND fcm_token IS NOT NULL",
          [userIds]
        );

        if (!rows || rows.length === 0) return;

        const tokens = rows.map((r) => r.fcm_token).filter(Boolean);
        if (tokens.length === 0) return;

        if (!initFirebase()) {
          console.log(`[FCM Mock Multicast] Sent to ${tokens.length} users: ${title}`);
          return;
        }

        const stringifiedData = {
          type: String(type),
          title: String(title || ""),
          body: String(body || ""),
          channelId: String(channelId),
        };
        for (const [key, value] of Object.entries(data)) {
          if (value !== undefined && value !== null) {
            stringifiedData[key] = typeof value === "object" ? JSON.stringify(value) : String(value);
          }
        }

        // Chunk in 500 batches per FCM specification
        const CHUNK_SIZE = 500;
        for (let i = 0; i < tokens.length; i += CHUNK_SIZE) {
          const chunk = tokens.slice(i, i + CHUNK_SIZE);
          const multicastMessage = {
            tokens: chunk,
            notification: { title, body },
            data: stringifiedData,
            android: {
              priority: "normal",
              notification: {
                channelId: channelId,
                sound: "default",
                color: "#6366F1",
              },
            },
          };

          const batchResponse = await getMessaging().sendEachForMulticast(multicastMessage);
          console.log(`[FCM Multicast Batch] Success: ${batchResponse.successCount}, Failures: ${batchResponse.failureCount}`);
        }
      } catch (err) {
        console.error("[NotificationService] Multicast error:", err.message);
      }
    });
  }

  /**
   * Broadcast to 10 Lakh+ Users via Google FCM Topic (Single API Call, Instant Delivery)
   * This is how Twitter, Instagram, and Telegram scale marketing/offers to millions.
   */
  static broadcastTopic(topic = "all_users", { title, body, imageUrl = null, data = {}, channelId = "camverz_marketing" }) {
    setImmediate(async () => {
      if (!initFirebase()) {
        console.log(`[FCM Mock Topic: ${topic}] Broadcast: ${title} - ${body}`);
        return;
      }

      try {
        const stringifiedData = {
          type: "marketing",
          title: String(title || ""),
          body: String(body || ""),
          channelId: String(channelId),
        };
        if (imageUrl) stringifiedData.imageUrl = String(imageUrl);

        for (const [key, value] of Object.entries(data)) {
          if (value !== undefined && value !== null) {
            stringifiedData[key] = typeof value === "object" ? JSON.stringify(value) : String(value);
          }
        }

        const message = {
          topic: topic,
          notification: {
            title: title,
            body: body,
            imageUrl: imageUrl || undefined,
          },
          data: stringifiedData,
          android: {
            priority: "normal",
            notification: {
              channelId: channelId,
              sound: "default",
              color: "#6366F1",
              imageUrl: imageUrl || undefined,
            },
          },
        };

        const response = await getMessaging().send(message);
        console.log(`[FCM Topic Broadcast Success] Topic '${topic}' message ID: ${response}`);
      } catch (err) {
        console.error(`[FCM Topic Broadcast Error] Topic '${topic}':`, err.message);
      }
    });
  }
}

module.exports = NotificationService;
