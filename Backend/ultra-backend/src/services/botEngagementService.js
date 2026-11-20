const http = require("http");
const https = require("https");
const { query, queryOne, queryMany } = require("../config/database");
const NotificationService = require("./notificationService");

/**
 * =========================================================================
 * BOT SEEDER
 * Ensures 50+ pre-defined bot users exist in the PostgreSQL users table.
 * =========================================================================
 */
async function ensureBotsSeeded() {
  try {
    for (const bot of BOT_PROFILES) {
      const existing = await queryOne("SELECT id FROM users WHERE google_id = $1", [bot.google_id]);
      if (!existing) {
        await query(
          `INSERT INTO users (google_id, email, name, gender, avatar, photo_url, bio, city, verified, is_bot, bot_personality)
           VALUES ($1, $2, $3, $4, $5, null, $6, $7, $8, true, $9)`,
          [
            bot.google_id,
            bot.email,
            bot.name,
            bot.gender,
            bot.avatar,
            bot.bio,
            bot.city,
            bot.verified,
            bot.personality,
          ]
        );
      } else {
        // Enforce app-internal avatar and clear external photo_url
        await query(
          `UPDATE users SET avatar = $1, photo_url = NULL, gender = $2, name = $3 WHERE google_id = $4 AND is_bot = true`,
          [bot.avatar, bot.gender, bot.name, bot.google_id]
        );
      }
    }
    await query("UPDATE users SET photo_url = NULL WHERE is_bot = true");
    console.log(`🤖 Seeded ${BOT_PROFILES.length} Diverse Production Bot Profiles (Using Native App Avatars Only)!`);
  } catch (err) {
    console.error("Error seeding bot profiles:", err.message);
  }
}

/**
 * =========================================================================
 * GET MATCHING BOTS FOR A USER (STRICT SEXUAL PREFERENCE & GENDER FILTER)
 * =========================================================================
 */
async function getMatchingBots(userGender, userPreference, count = 4) {
  try {
    let genderFilter = "";
    const cleanPref = (userPreference || "straight").toLowerCase();
    const cleanGen = (userGender || "male").toLowerCase();

    if (cleanPref === "gay") {
      // Gay male -> attracted to males -> all male bots
      genderFilter = "WHERE is_bot = true AND gender = 'male'";
    } else if (cleanPref === "lesbian") {
      // Lesbian female -> attracted to females -> all female bots
      genderFilter = "WHERE is_bot = true AND gender = 'female'";
    } else if (cleanGen === "female") {
      // Straight female -> attracted to males -> all male bots
      genderFilter = "WHERE is_bot = true AND gender = 'male'";
    } else {
      // Straight male -> attracted to females -> all female bots
      genderFilter = "WHERE is_bot = true AND gender = 'female'";
    }

    const bots = await queryMany(
      `SELECT id, name, gender, avatar, photo_url, bot_personality
       FROM users ${genderFilter}
       ORDER BY RANDOM() LIMIT $1`,
      [count]
    );

    if (bots && bots.length > 0) return bots;

    // Fallback if specific filter has no bots
    return await queryMany("SELECT id, name, gender, avatar, photo_url, bot_personality FROM users WHERE is_bot = true ORDER BY RANDOM() LIMIT $1", [count]);
  } catch (err) {
    console.error("Error fetching matching bots:", err);
    return [];
  }
}

/**
 * =========================================================================
 * SCHEDULE AUTOMATED POST ENGAGEMENT (MULTI-WAVE ENGAGEMENT OVER 10 MINS)
 * =========================================================================
 */
function schedulePostEngagement(postId, authorUserId, postText, category, userGender, sexPreference) {
  setImmediate(async () => {
    try {
      // Don't auto-engage on posts made by bots themselves
      const author = await queryOne("SELECT is_bot, country, gender, sex_preference FROM users WHERE id = $1", [authorUserId]);
      if (author && author.is_bot) return;

      const userGen = (author && author.gender ? author.gender : userGender || "male").toLowerCase();
      const userPref = (author && author.sex_preference ? author.sex_preference : sexPreference || "straight").toLowerCase();

      const isIndianUser = !author || !author.country || author.country.toLowerCase() === "india";

      // If user is from India, mix English and Hinglish across bots
      const bot1UsesHinglish = isIndianUser && Math.random() < 0.5;
      const bot2UsesHinglish = isIndianUser && !bot1UsesHinglish;

      const analysis = analyzePostText(postText);
      const bots = await getMatchingBots(userGen, userPref, 5);
      if (!bots || bots.length === 0) return;

      const bot1 = bots[0];
      const bot2 = bots.length > 1 ? bots[1] : null;
      const bot3 = bots.length > 2 ? bots[2] : null;
      const bot4 = bots.length > 3 && Math.random() < 0.85 ? bots[3] : null;
      const bot5 = bots.length > 4 && Math.random() < 0.65 ? bots[4] : null;

      // -------------------------------------------------------------
      // BOT 1: First Reaction (Like in 20-35s, Comment in 35-65s)
      // -------------------------------------------------------------
      const bot1LikeDelay = randomBetween(20, 35) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot1.id]);

          await query(
            `INSERT INTO notifications (user_id, type, triggering_user_id, post_id)
             VALUES ($1, 'like', $2, $3)`,
            [authorUserId, bot1.id, postId]
          );

          NotificationService.sendToUser(authorUserId, {
            title: "New Like ❤️",
            body: `${bot1.name} liked your post!`,
            type: "like",
            channelId: "camverz_social",
            data: { postId, triggeringUserId: bot1.id },
          });
        } catch (e) {
          console.error("Bot1 like error:", e.message);
        }
      }, bot1LikeDelay);

      const bot1CommentDelay = randomBetween(35, 65) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          const commentText = await generateContextualComment(analysis, bot1.bot_personality, bot1.name, isIndianUser, bot1UsesHinglish, userGen, userPref, bot1.gender);
            if (!commentText || !commentText.trim()) return;

            const comment = await queryOne(
            `INSERT INTO comments (post_id, user_id, text) VALUES ($1, $2, $3) RETURNING *`,
            [postId, bot1.id, commentText]
          );

          await query(
            `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
             VALUES ($1, 'comment', $2, $3, $4)`,
            [authorUserId, bot1.id, postId, comment.id]
          );

          NotificationService.sendToUser(authorUserId, {
            title: "New Comment 💬",
            body: `${bot1.name}: ${commentText.substring(0, 80)}`,
            type: "comment",
            channelId: "camverz_social",
            data: { postId, commentId: comment.id, triggeringUserId: bot1.id },
          });

          console.log(`💬 Bot1 (${bot1.name}) [${bot1UsesHinglish ? "Hinglish" : "English"}] commented on post ${postId}: "${commentText}"`);
        } catch (e) {
          console.error("Bot1 comment error:", e.message);
        }
      }, bot1CommentDelay);

      // -------------------------------------------------------------
      // BOT 2: Second Reaction (Like in 75-110s, Comment in 95-140s)
      // -------------------------------------------------------------
      if (bot2) {
        const bot2LikeDelay = randomBetween(75, 110) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot2.id]);

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id)
               VALUES ($1, 'like', $2, $3)`,
              [authorUserId, bot2.id, postId]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Like ❤️",
              body: `${bot2.name} liked your post!`,
              type: "like",
              channelId: "camverz_social",
              data: { postId, triggeringUserId: bot2.id },
            });
          } catch (e) {
            console.error("Bot2 like error:", e.message);
          }
        }, bot2LikeDelay);

        const bot2CommentDelay = randomBetween(95, 140) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            const commentText = await generateContextualComment(analysis, bot2.bot_personality, bot2.name, isIndianUser, bot2UsesHinglish, userGen, userPref, bot2.gender);
            if (!commentText || !commentText.trim()) return;

            const comment = await queryOne(
              `INSERT INTO comments (post_id, user_id, text) VALUES ($1, $2, $3) RETURNING *`,
              [postId, bot2.id, commentText]
            );

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
               VALUES ($1, 'comment', $2, $3, $4)`,
              [authorUserId, bot2.id, postId, comment.id]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Comment 💬",
              body: `${bot2.name}: ${commentText.substring(0, 80)}`,
              type: "comment",
              channelId: "camverz_social",
              data: { postId, commentId: comment.id, triggeringUserId: bot2.id },
            });

            console.log(`💬 Bot2 (${bot2.name}) [${bot2UsesHinglish ? "Hinglish" : "English"}] commented on post ${postId}: "${commentText}"`);
          } catch (e) {
            console.error("Bot2 comment error:", e.message);
          }
        }, bot2CommentDelay);
      }

      // -------------------------------------------------------------
      // BOT 3: Third Reaction (Like in 160-220s, Comment in 190-260s)
      // -------------------------------------------------------------
      if (bot3) {
        const bot3UsesHinglish = isIndianUser && Math.random() < 0.5;
        const bot3LikeDelay = randomBetween(160, 220) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot3.id]);

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id)
               VALUES ($1, 'like', $2, $3)`,
              [authorUserId, bot3.id, postId]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Like ❤️",
              body: `${bot3.name} liked your post!`,
              type: "like",
              channelId: "camverz_social",
              data: { postId, triggeringUserId: bot3.id },
            });
          } catch (e) {
            console.error("Bot3 like error:", e.message);
          }
        }, bot3LikeDelay);

        const bot3CommentDelay = randomBetween(190, 260) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            const commentText = await generateContextualComment(analysis, bot3.bot_personality, bot3.name, isIndianUser, bot3UsesHinglish, userGen, userPref, bot3.gender);
            if (!commentText || !commentText.trim()) return;

            const comment = await queryOne(
              `INSERT INTO comments (post_id, user_id, text) VALUES ($1, $2, $3) RETURNING *`,
              [postId, bot3.id, commentText]
            );

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
               VALUES ($1, 'comment', $2, $3, $4)`,
              [authorUserId, bot3.id, postId, comment.id]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Comment 💬",
              body: `${bot3.name}: ${commentText.substring(0, 80)}`,
              type: "comment",
              channelId: "camverz_social",
              data: { postId, commentId: comment.id, triggeringUserId: bot3.id },
            });

            console.log(`💬 Bot3 (${bot3.name}) [${bot3UsesHinglish ? "Hinglish" : "English"}] commented on post ${postId}: "${commentText}"`);
          } catch (e) {
            console.error("Bot3 comment error:", e.message);
          }
        }, bot3CommentDelay);
      }

      // -------------------------------------------------------------
      // BOT 4: Fourth Reaction (Like in 280-360s, Comment in 330-420s)
      // -------------------------------------------------------------
      if (bot4) {
        const bot4UsesHinglish = isIndianUser && Math.random() < 0.5;
        const bot4LikeDelay = randomBetween(280, 360) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot4.id]);

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id)
               VALUES ($1, 'like', $2, $3)`,
              [authorUserId, bot4.id, postId]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Like ❤️",
              body: `${bot4.name} liked your post!`,
              type: "like",
              channelId: "camverz_social",
              data: { postId, triggeringUserId: bot4.id },
            });
          } catch (e) {
            console.error("Bot4 like error:", e.message);
          }
        }, bot4LikeDelay);

        const bot4CommentDelay = randomBetween(330, 420) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            const commentText = await generateContextualComment(analysis, bot4.bot_personality, bot4.name, isIndianUser, bot4UsesHinglish, userGen, userPref, bot4.gender);
            if (!commentText || !commentText.trim()) return;

            const comment = await queryOne(
              `INSERT INTO comments (post_id, user_id, text) VALUES ($1, $2, $3) RETURNING *`,
              [postId, bot4.id, commentText]
            );

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
               VALUES ($1, 'comment', $2, $3, $4)`,
              [authorUserId, bot4.id, postId, comment.id]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Comment 💬",
              body: `${bot4.name}: ${commentText.substring(0, 80)}`,
              type: "comment",
              channelId: "camverz_social",
              data: { postId, commentId: comment.id, triggeringUserId: bot4.id },
            });

            console.log(`💬 Bot4 (${bot4.name}) [${bot4UsesHinglish ? "Hinglish" : "English"}] commented on post ${postId}: "${commentText}"`);
          } catch (e) {
            console.error("Bot4 comment error:", e.message);
          }
        }, bot4CommentDelay);
      }

      // -------------------------------------------------------------
      // BOT 5: Fifth Reaction (Like in 420-540s, Comment in 500-650s)
      // -------------------------------------------------------------
      if (bot5) {
        const bot5UsesHinglish = isIndianUser && Math.random() < 0.5;
        const bot5LikeDelay = randomBetween(420, 540) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot5.id]);

            NotificationService.sendToUser(authorUserId, {
              title: "New Like ❤️",
              body: `${bot5.name} liked your post!`,
              type: "like",
              channelId: "camverz_social",
              data: { postId, triggeringUserId: bot5.id },
            });
          } catch (e) {
            console.error("Bot5 like error:", e.message);
          }
        }, bot5LikeDelay);

        const bot5CommentDelay = randomBetween(500, 650) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            const commentText = await generateContextualComment(analysis, bot5.bot_personality, bot5.name, isIndianUser, bot5UsesHinglish, userGen, userPref, bot5.gender);
            if (!commentText || !commentText.trim()) return;

            const comment = await queryOne(
              `INSERT INTO comments (post_id, user_id, text) VALUES ($1, $2, $3) RETURNING *`,
              [postId, bot5.id, commentText]
            );

            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
               VALUES ($1, 'comment', $2, $3, $4)`,
              [authorUserId, bot5.id, postId, comment.id]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Comment 💬",
              body: `${bot5.name}: ${commentText.substring(0, 80)}`,
              type: "comment",
              channelId: "camverz_social",
              data: { postId, commentId: comment.id, triggeringUserId: bot5.id },
            });

            console.log(`💬 Bot5 (${bot5.name}) [${bot5UsesHinglish ? "Hinglish" : "English"}] commented on post ${postId}: "${commentText}"`);
          } catch (e) {
            console.error("Bot5 comment error:", e.message);
          }
        }, bot5CommentDelay);
      }
    } catch (err) {
      console.error("Post engagement scheduler error:", err);
    }
  });
}

/**
 * =========================================================================
 * SCHEDULE AUTOMATED STORY VIEWS (LEVEL 1 STORY ENGAGEMENT)
 * Staggers 5 to 12 realistic story views across 5-25 minutes!
 * =========================================================================
 */
function scheduleStoryViews(storyId, authorUserId) {
  setImmediate(async () => {
    try {
      const author = await queryOne("SELECT is_bot, gender, sex_preference FROM users WHERE id = $1", [authorUserId]);
      if (author && author.is_bot) return;

      const userGen = (author && author.gender ? author.gender : "male").toLowerCase();
      const userPref = (author && author.sex_preference ? author.sex_preference : "straight").toLowerCase();

      // Pick 8 to 12 matching bot viewers
      const viewerCount = randomBetween(6, 12);
      const bots = await getMatchingBots(userGen, userPref, viewerCount);
      if (!bots || bots.length === 0) return;

      // Stagger each view smoothly
      bots.forEach((bot, index) => {
        const viewDelay = (randomBetween(30, 90) + (index * randomBetween(45, 90))) * 1000;
        setTimeout(async () => {
          try {
            const storyStillExists = await queryOne("SELECT id FROM stories WHERE id = $1", [storyId]);
            if (!storyStillExists) return;

            await query(
              `INSERT INTO story_views (story_id, viewer_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
              [storyId, bot.id]
            );
          } catch (e) {
            console.error(`Story view error for bot ${bot.name}:`, e.message);
          }
        }, viewDelay);
      });

      console.log(`👁️ Scheduled ${bots.length} automated story views for story ${storyId}`);
    } catch (err) {
      console.error("Story engagement scheduler error:", err);
    }
  });
}

/**
 * =========================================================================
 * 2-WAY INTERACTIVE THREADING (USER REPLIES TO BOT COMMENT)
 * If the user replies to a bot's comment, the bot replies back in 45-75s!
 * =========================================================================
 */
function handleUserReplyToBot(postId, parentCommentId, replyingUserId, userReplyText) {
  setImmediate(async () => {
    try {
      if (!parentCommentId) return;

      // Check if the parent comment was posted by a bot
      const parentComment = await queryOne(
        `SELECT c.id, c.user_id, u.is_bot, u.name, u.bot_personality, u.gender
         FROM comments c
         JOIN users u ON u.id = c.user_id
         WHERE c.id = $1`,
        [parentCommentId]
      );

      if (!parentComment || !parentComment.is_bot) return;

      // Don't loop if bot is replying to bot
      const replier = await queryOne("SELECT is_bot, country, gender, sex_preference FROM users WHERE id = $1", [replyingUserId]);
      if (replier && replier.is_bot) return;

      const isIndianUser = !replier || !replier.country || replier.country.toLowerCase() === "india";
      const replyInHinglish = isIndianUser && Math.random() < 0.5;

      const replyDelay = randomBetween(45, 75) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          // Generate natural conversational reply using Groq Cloud, Local Ollama or fallback
          let botReplyText = "";
          const langDesc = replyInHinglish
            ? "natural casual conversational Hinglish (blend of Hindi & English in English letters, like 'Haha bilkul sahi bola')"
            : "natural casual conversational English";
          const replySystem = `You are ${parentComment.name}, a user on a social app called Camverz.
Reply to the user directly in a friendly, engaging, natural human way.
Language: ${langDesc}.
Length: Exactly 1 short sentence (8-14 words).
Include 1 emoji.
Output ONLY the reply text. Do NOT include quotation marks.`;

          // 1. Try Groq Cloud first (Free for Render)
          try {
            botReplyText = await callGroqAI(replySystem, `User replied to your comment: "${userReplyText}"`);
          } catch (e) {}

          // 2. Try Local Ollama (Local PC / VPS)
          if (!botReplyText) {
            try {
              const replyPrompt = `${replySystem}\nUser replied: "${userReplyText}"`;
              botReplyText = await callLocalOllamaAI(replyPrompt, 20000);
            } catch (e) {}
          }

          if (!botReplyText || !botReplyText.trim()) return;

          const inserted = await queryOne(
            `INSERT INTO comments (post_id, user_id, text, parent_id)
             VALUES ($1, $2, $3, $4) RETURNING *`,
            [postId, parentComment.user_id, botReplyText, parentCommentId]
          );

          await query(
            `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id)
             VALUES ($1, 'reply', $2, $3, $4)`,
            [replyingUserId, parentComment.user_id, postId, inserted.id]
          );

          NotificationService.sendToUser(replyingUserId, {
            title: "New Reply 💬",
            body: `${parentComment.name}: ${botReplyText.substring(0, 80)}`,
            type: "reply",
            channelId: "camverz_social",
            data: { postId, commentId: inserted.id, triggeringUserId: parentComment.user_id },
          });

          console.log(`💬 Bot (${parentComment.name}) replied back to user on comment ${parentCommentId}: "${botReplyText}"`);
        } catch (e) {
          console.error("Bot reply back error:", e.message);
        }
      }, replyDelay);
    } catch (err) {
      console.error("Handle user reply to bot error:", err);
    }
  });
}

/**
 * =========================================================================
 * AUTO-CATCHUP ENGINE (100% ENGAGEMENT GUARANTEE)
 * Periodically checks for recent user posts (< 20 mins) with < 2 comments
 * and automatically backfills engagement if any timer was missed.
 * =========================================================================
 */
async function runAutoCatchupWorker() {
  try {
    const recentPosts = await queryMany(
      `SELECT p.id, p.user_id, p.text, p.category, u.gender, u.sex_preference, u.country,
              (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) as comment_count
       FROM posts p
       JOIN users u ON u.id = p.user_id
       WHERE p.created_at >= NOW() - INTERVAL '20 minutes'
         AND u.is_bot = false
       ORDER BY p.created_at DESC LIMIT 15`
    );

    for (const post of recentPosts) {
      const count = parseInt(post.comment_count || "0");
      if (count < 2) {
        console.log(`⚡ Catchup Worker: Boosting under-commented post ${post.id}`);
        schedulePostEngagement(
          post.id,
          post.user_id,
          post.text,
          post.category,
          post.gender,
          post.sex_preference
        );
      }
    }
  } catch (err) {
    // Non-blocking catchup
  }
}

// Start Catchup Worker interval (every 4 minutes)
setInterval(runAutoCatchupWorker, 4 * 60 * 1000);

module.exports = {
  ensureBotsSeeded,
  schedulePostEngagement,
  scheduleStoryViews,
  handleUserReplyToBot,
  analyzePostText,
  generateContextualComment,
  runAutoCatchupWorker,
};
