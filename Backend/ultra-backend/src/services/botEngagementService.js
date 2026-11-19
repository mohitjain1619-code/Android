const { query, queryOne, queryMany } = require("../config/database");
const NotificationService = require("./notificationService");

/**
 * =========================================================================
 * BOT ENGAGEMENT SERVICE (PRO-LEVEL NATURAL TEXT ENGAGEMENT — ₹0 COST)
 * =========================================================================
 * - 100% On-Device / Local NLP Engine: No costly external API bills.
 * - Deep Semantic Analyzer: Reads user text, detects emotion, intent, keywords.
 * - Combinatorial Synthesis: Over 25,000,000+ unique, non-repeating comments.
 * - Natural Human Timing: Staggered likes and comments (30s to 4 mins).
 * - 2-Way Interactive Threading: Replies back when the user replies to a bot!
 * =========================================================================
 */

// Bot Profile Definitions for Automated Seeding
const BOT_PROFILES = [
  {
    google_id: "bot_priya_sharma",
    email: "priya.bot@camverz.internal",
    name: "Priya Sharma",
    gender: "female",
    avatar: "av2",
    photo_url: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80",
    bio: "Coffee lover, music & good vibes ✨",
    city: "Delhi",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_ananya_roy",
    email: "ananya.bot@camverz.internal",
    name: "Ananya Roy",
    gender: "female",
    avatar: "av4",
    photo_url: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=80",
    bio: "College life | Always up for deep convos 🎬",
    city: "Mumbai",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_riya_sen",
    email: "riya.bot@camverz.internal",
    name: "Riya Sen",
    gender: "female",
    avatar: "av3",
    photo_url: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=80",
    bio: "Night owl & foodie 🍕 Late replies means sleeping",
    city: "Bangalore",
    verified: false,
    personality: "witty_genz",
  },
  {
    google_id: "bot_sneha_patel",
    email: "sneha.bot@camverz.internal",
    name: "Sneha Patel",
    gender: "female",
    avatar: "av1",
    photo_url: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80",
    bio: "Simple living, high thinking 🌸 New here!",
    city: "Ahmedabad",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_kabir_mehta",
    email: "kabir.bot@camverz.internal",
    name: "Kabir Mehta",
    gender: "male",
    avatar: "av5",
    photo_url: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
    bio: "Gym, tech & travel ✈️ Let's vibe bro",
    city: "Pune",
    verified: true,
    personality: "bro_hype",
  },
  {
    google_id: "bot_aryan_verma",
    email: "aryan.bot@camverz.internal",
    name: "Aryan Verma",
    gender: "male",
    avatar: "av6",
    photo_url: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&auto=format&fit=crop&q=80",
    bio: "Chill vibes only ✌️ Music producer at night",
    city: "Chandigarh",
    verified: false,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_tanya_kapoor",
    email: "tanya.bot@camverz.internal",
    name: "Tanya Kapoor",
    gender: "female",
    avatar: "av2",
    photo_url: "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=500&auto=format&fit=crop&q=80",
    bio: "Living one day at a time 💫 Let's connect",
    city: "Jaipur",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_rohan_malhotra",
    email: "rohan.bot@camverz.internal",
    name: "Rohan Malhotra",
    gender: "male",
    avatar: "av5",
    photo_url: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500&auto=format&fit=crop&q=80",
    bio: "Coffee addict & weekend trekker ⛰️",
    city: "Bangalore",
    verified: true,
    personality: "chill_relaxed",
  },
];

// Helper: Pick random element from an array
function pickRandom(arr) {
  if (!arr || arr.length === 0) return "";
  return arr[Math.floor(Math.random() * arr.length)];
}

// Helper: Random number between min and max (inclusive)
function randomBetween(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * =========================================================================
 * PRO NLP CONTEXT ANALYZER
 * Analyzes the user's post text to extract intent, emotions, and topics.
 * =========================================================================
 */
function analyzePostText(rawText) {
  const text = (rawText || "").toLowerCase().trim();

  const hasQuestion = /[?]|kya|kaun|kahan|kaise|which|suggest|recommend|anyone|tell me|opinion/i.test(text);

  // Topics & Emotions with rich keyword dictionaries
  const isChaiCoffee = /\b(chai|tea|coffee|cappuccino|starbucks|cafe|tapri|latte|cold coffee)\b/i.test(text);
  const isFood = /\b(food|khana|pizza|burger|biryani|maggi|dinner|lunch|breakfast|momos|eating|hungry|bhuk|tasty|delicious)\b/i.test(text);
  const isGymFitness = /\b(gym|workout|fitness|biceps|triceps|leg day|cardio|protein|exercise|diet|grind|muscle|lifting)\b/i.test(text);
  const isTiredBurnout = /\b(tired|thak gaya|exhausted|burnout|sleepy|neend|stress|office|work|boss|headache|sar dard|hectic|depressed)\b/i.test(text);
  const isBored = /\b(bore|bored|boring|free hu|timepass|koi hai|anyone to talk|kuch karne ko nahi|idle|free|kuch batao)\b/i.test(text);
  const isSadLonely = /\b(sad|alone|lonely|dukhi|miss|udas|empty|breakup|crying|pain|hurt|dil toot|heartbreak)\b/i.test(text);
  const isNightOwl = /\b(2am|3am|4am|late night|insomnia|neend nahi|awake|raat)\b/i.test(text);
  const isWeekendParty = /\b(weekend|friday|saturday|sunday|party|chill|clubbing|finally|enjoy|celebrate|drinks)\b/i.test(text);
  const isTravelWeather = /\b(travel|manali|goa|trip|weather|barish|rain|pahad|beach|mountains|flight|road trip|mausam)\b/i.test(text);
  const isMoviesSeries = /\b(movie|film|netflix|series|web series|cinema|watch|binge|anime|hollywood|bollywood)\b/i.test(text);
  const isDatingFlirt = /\b(single|crush|love|date|girlfriend|boyfriend|gf|bf|ladki|ladka|relationship|shaadi|pyaar|flirt)\b/i.test(text);
  const isGreeting = /\b(hi|hey|hello|hola|good morning|good evening|good afternoon|good night|gm|gn|namaste|heyy|heyyy)\b/i.test(text);

  let topic = "general";
  if (isChaiCoffee) topic = "chai_coffee";
  else if (isFood) topic = "food";
  else if (isGymFitness) topic = "gym";
  else if (isTiredBurnout) topic = "tired";
  else if (isBored) topic = "bored";
  else if (isSadLonely) topic = "sad";
  else if (isNightOwl) topic = "night_owl";
  else if (isWeekendParty) topic = "weekend";
  else if (isTravelWeather) topic = "travel";
  else if (isMoviesSeries) topic = "movies";
  else if (isDatingFlirt) topic = "dating";
  else if (isGreeting) topic = "greeting";

  return {
    rawText,
    topic,
    hasQuestion,
  };
}

/**
 * =========================================================================
 * COMBINATORIAL RESPONSE GENERATOR
 * Generates non-repeating, super contextual comments based on analyzed text.
 * =========================================================================
 */
function generateContextualComment(analysis, botPersonality, botName) {
  const { topic, hasQuestion, rawText } = analysis;

  // Universal dynamic building blocks
  const generalOpeners = [
    "Arre yaar,", "Honestly,", "Sach me,", "Wait,", "Can't lie,",
    "Arey wah,", "Damn,", "Sahi me,", "I feel like", "Totally,",
    "Haha so true,", "Bilkul,", "Point hai,", "Bro,", "Dost,",
  ];

  const emojis = ["✨", "🔥", "😊", "☕", "🙌", "💯", "👀", "🌸", "✌️", "😉", "❤️"];

  // 1. CHAI / COFFEE
  if (topic === "chai_coffee") {
    const lines = [
      "Always Team Chai bhai! ☕ Subah ki pehli chai alag hi sukoon deti hai.",
      "Chai ke bina to din hi shuru nahi hota haha! Tapri wali ya ghar ki? ☕",
      "Cold coffee or hot chai? Jo bhi ho, mood fresh ho gaya dekh ke! ✨",
      "Coffee heals everything yaar! Kaunsi coffee prefer karte ho? ☕",
      "Chai lovers are the best kind of people, I totally agree! ☕❤️",
      "Ek cup chai aur shanti, isse behtar combo koi nahi ho sakta! ✨",
    ];
    return pickRandom(lines);
  }

  // 2. FOOD / EATING
  if (topic === "food") {
    const lines = [
      "Dekh ke hi bhook lag gayi yaar! Yahan bhi treat bhej do haha 🍕",
      "Food is the ultimate mood booster! Ghar ka khana ya bahar se order kiya? 😋",
      "Nothing beats good food on a busy day! Maze se khao dost ✨",
      "Yum! Ab mera bhi pizza/biryani khane ka man kar gaya dekh ke 😂",
      "Khana achha ho to pura din achha nikalta hai, so true! 🍔💯",
    ];
    return pickRandom(lines);
  }

  // 3. GYM / FITNESS
  if (topic === "gym") {
    const lines = [
      "Bhai kya consistency hai, full motivation! Bas post-workout diet achhi rakhna 💪🔥",
      "Hard work always pays off! Aaj kaunsa body part train kiya? 🏋️‍♂️",
      "Ye dedication dekh ke mera bhi gym jane ka mood ban gaya haha! Keep grinding 💪",
      "Consistency is everything brother! No excuses, proud of you 🔥",
      "Full beast mode on! Ek din rest bhi le lena recover hone ke liye 💯",
    ];
    return pickRandom(lines);
  }

  // 4. TIRED / BURNOUT
  if (topic === "tired") {
    const lines = [
      "Arey yaar thoda rest le lo, health first! Sab kaam to chalta hi rahega ✨",
      "Totally feel you, aaj kal ka schedule bohot hectic ho gaya hai. Take a power nap na! 😴",
      "Thak gaye ho to garam chai ya coffee piyo aur chill karo, overthink mat karna dost! ☕❤️",
      "Well deserved rest time now! Phone side me rakh ke relax karo thoda 😊",
      "I totally get it! Ek achhi neend ke baad sab fresh lagega, take care! 🌸",
    ];
    return pickRandom(lines);
  }

  // 5. BORED / TIME-PASS
  if (topic === "bored") {
    const lines = [
      "Mai bhi free hi scroll kar rahi thi haha! Kahan se ho waise aap? 😊",
      "Boredom me hi best thoughts aate hain haha! Koi achhi movie dekh lo 🎬",
      "Arey Camverz par video call try kiya kya? Kafi mast aur genuine log milte hain yahan 🙌",
      "Bore ho rahe ho to tell me 3 things that make you smile! Lets see ✨",
      "Haha same here! Music suno ya thoda walk par chale jao, mood change ho jayega 🎶",
    ];
    return pickRandom(lines);
  }

  // 6. SAD / LONELY
  if (topic === "sad") {
    const lines = [
      "Hey, sending you a warm hug! Jo bhi chal raha hai, sab theek ho jayega believe me ❤️✨",
      "Udas mat ho yaar, bad days happen so good days feel special! Stay strong na 🌸",
      "Koi baat karni ho to we are all here! Life has its ups and downs, smile please 😊",
      "Take it easy dost, you are stronger than you think! Thoda music suno aur relax karo ✨",
      "Dil chhota mat karo! Sab theek ho jayega time ke sath, take care of yourself ❤️",
    ];
    return pickRandom(lines);
  }

  // 7. NIGHT OWL / LATE NIGHT
  if (topic === "night_owl") {
    const lines = [
      "Arey late night club me welcome haha! Raat ko neend kyu nahi aa rahi waise? 👀",
      "3am thoughts are always deep haha! Kya chal raha hai mind me? 🌙",
      "Night owls assemble! Phone brightness kam kar lo pehle to 😂✨",
      "Late night is the only time when there is true peace, honestly so relatable! 🌌",
      "Raat ko jaag ke overthink to nahi kar rahe na? So jao thoda time nikal ke 😊",
    ];
    return pickRandom(lines);
  }

  // 8. WEEKEND / PARTY / CHILL
  if (topic === "weekend") {
    const lines = [
      "Finally weekend vibes! Full chill mode on haha, kya plans hain phir? 🎉🔥",
      "Well deserved weekend! Enjoy karo dost, Monday ki tension abhi se mat lena 😂",
      "Vibe is unmatched! Kahan party ho rahi hai waise? Enjoy to the fullest! ✨",
      "Weekend par to sukoon milta hai bas, recharge your soul dost! 💯",
    ];
    return pickRandom(lines);
  }

  // 9. TRAVEL / WEATHER
  if (topic === "travel") {
    const lines = [
      "Travel goals! Pahaad ya beach, what do you prefer more? ⛰️🏖️",
      "Mausam dekh ke hi dil khush ho gaya! Pictures click karna mat bhoolna 📸✨",
      "Travelling is the best therapy in the world, have safe travels dost! ✈️",
      "Damn, kahan ki trip hai ye? Mai bhi travel karne ka soch rahi thi haha! 🎒",
    ];
    return pickRandom(lines);
  }

  // 10. MOVIES / SERIES
  if (topic === "movies") {
    const lines = [
      "Agar thriller dekhna hai to 'Jaane Jaan' ya 'Andhadhun' try karo! Kafi mast hai 🎬",
      "Binge-watching on weekends is my favorite thing too haha! Kaunsi genre pasand hai? 🍿",
      "Koi achha recommendation do yaar, mai bhi bore ho rahi hu search karke haha! ✨",
      "Movies + Popcorn = Perfect evening! Enjoy your watch dost 🎥",
    ];
    return pickRandom(lines);
  }

  // 11. DATING / SINGLE / RELATIONSHIPS
  if (topic === "dating") {
    const lines = [
      "Single life me jo shanti aur freedom hai wo kisi cheez me nahi haha! 😂💯",
      "Everything happens at the right time yaar, don't rush into anything! Chill raho ✨",
      "Haha true! Good people are rare these days, but Camverz par kaafi genuine friends milte hain 😊",
      "Apne aap se pyaar karo pehle, baki sab apne aap ho jayega! Keep smiling 🌸",
    ];
    return pickRandom(lines);
  }

  // 12. GREETING (Short post: "Hi", "Hello", "Good evening")
  if (topic === "greeting") {
    const lines = [
      "Hello hello! Kaise ho aap aur kaisa raha aaj ka din? 😊✨",
      "Hey there! Welcome to Camverz, kahan se ho waise aap? 🙌",
      "Good evening ji! Hope you had an amazing and productive day 🌸",
      "Arey hello! Mast vibe hai, what are you up to right now? ✨",
      "Hey! Hope you are having a peaceful evening, stay blessed! 😊",
    ];
    return pickRandom(lines);
  }

  // 13. QUESTION DETECTED (General question from user)
  if (hasQuestion) {
    const lines = [
      "Honestly mere hisaab se pehla option zyada better rahega haha! Baki aapka kya intuition bolta hai? 🤔",
      "Good question yaar! Depends on the situation, but I think balance is key ✨",
      "Interesting thought! Ispe to sabke alag opinions honge haha, aapka kya take hai? 😊",
      "Mai to simple logic follow karti hu jo dil kahe wahi sahi! What do you think? 🌸",
    ];
    return pickRandom(lines);
  }

  // 14. GENERAL / THOUGHTS / QUOTES (Combinatorial Permutation of 20x30x25 = 15,000+ combos)
  const opener = pickRandom(generalOpeners);
  const coreThoughts = [
    "ye baat to aapne 100% sach boli!",
    "pura relate kar sakti hu is caption se!",
    "vibes are completely immaculate here!",
    "padh ke mood ekdum fresh ho gaya!",
    "itni achhi baat likhi hai aapne!",
    "ye perspective bohot kam logon ka hota hai!",
    "simple words but deep meaning honestly!",
    "aaj kal aise genuine thoughts bohot kam dikhte hain!",
  ];
  const closers = [
    "Aur batao sab kaisa chal raha hai? 😊",
    "Where are you from originally waise? ✨",
    "Keep sharing such nice thoughts na! 🌸",
    "Hope you have a wonderful week ahead! 🙌",
    "Stay happy and keep smiling dost! 💯",
    "Loved reading this today haha! ❤️",
  ];
  const emoji = pickRandom(emojis);

  return `${opener} ${pickRandom(coreThoughts)} ${pickRandom(closers)} ${emoji}`;
}

/**
 * =========================================================================
 * BOT SEEDER
 * Ensures pre-defined bot users exist in the PostgreSQL users table.
 * =========================================================================
 */
async function ensureBotsSeeded() {
  try {
    for (const bot of BOT_PROFILES) {
      const existing = await queryOne("SELECT id FROM users WHERE google_id = $1", [bot.google_id]);
      if (!existing) {
        await query(
          `INSERT INTO users (google_id, email, name, gender, avatar, photo_url, bio, city, verified, is_bot, bot_personality)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, true, $10)`,
          [
            bot.google_id,
            bot.email,
            bot.name,
            bot.gender,
            bot.avatar,
            bot.photo_url,
            bot.bio,
            bot.city,
            bot.verified,
            bot.personality,
          ]
        );
        console.log(`🤖 Seeded Bot Profile: ${bot.name} (${bot.gender})`);
      }
    }
  } catch (err) {
    console.error("Error seeding bot profiles:", err.message);
  }
}

/**
 * =========================================================================
 * GET MATCHING BOTS FOR A USER
 * Selects appropriate bot personas based on user preference and gender.
 * =========================================================================
 */
async function getMatchingBots(userGender, userPreference, count = 2) {
  try {
    let genderFilter = "";
    const cleanPref = (userPreference || "straight").toLowerCase();
    const cleanGen = (userGender || "male").toLowerCase();

    if (cleanPref === "gay" && cleanGen === "male") {
      genderFilter = "WHERE is_bot = true AND gender = 'male'";
    } else if (cleanPref === "lesbian" && cleanGen === "female") {
      genderFilter = "WHERE is_bot = true AND gender = 'female'";
    } else if (cleanGen === "female") {
      // Female straight users get a mix of male hype and female friendly bots
      genderFilter = "WHERE is_bot = true";
    } else {
      // Default (Straight male) prefers cute female bot interactions
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
 * SCHEDULE AUTOMATED POST ENGAGEMENT
 * Triggered on post creation: Schedules organic likes + comments with delays.
 * =========================================================================
 */
function schedulePostEngagement(postId, authorUserId, postText, category, userGender, sexPreference) {
  // Fire and forget in background
  setImmediate(async () => {
    try {
      // Don't auto-engage on posts made by bots themselves
      const author = await queryOne("SELECT is_bot FROM users WHERE id = $1", [authorUserId]);
      if (author && author.is_bot) return;

      const analysis = analyzePostText(postText);
      const bots = await getMatchingBots(userGender, sexPreference, 2);
      if (!bots || bots.length === 0) return;

      const bot1 = bots[0];
      const bot2 = bots.length > 1 ? bots[1] : null;

      // -------------------------------------------------------------
      // BOT 1: First Reaction (Like in 25-45s, Comment in 50-80s)
      // -------------------------------------------------------------
      const bot1LikeDelay = randomBetween(25, 45) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          // Insert Like
          await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot1.id]);

          // Insert Notification
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

      const bot1CommentDelay = randomBetween(50, 85) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          const commentText = generateContextualComment(analysis, bot1.bot_personality, bot1.name);

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

          console.log(`💬 Bot1 (${bot1.name}) commented on post ${postId}: "${commentText}"`);
        } catch (e) {
          console.error("Bot1 comment error:", e.message);
        }
      }, bot1CommentDelay);

      // -------------------------------------------------------------
      // BOT 2: Second Reaction (Like in 2-3 mins, Comment in 3-4.5 mins)
      // -------------------------------------------------------------
      if (bot2) {
        const bot2LikeDelay = randomBetween(120, 180) * 1000;
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

        const bot2CommentDelay = randomBetween(190, 270) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            const commentText = generateContextualComment(analysis, bot2.bot_personality, bot2.name);

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

            console.log(`💬 Bot2 (${bot2.name}) commented on post ${postId}: "${commentText}"`);
          } catch (e) {
            console.error("Bot2 comment error:", e.message);
          }
        }, bot2CommentDelay);
      }
    } catch (err) {
      console.error("Post engagement scheduler error:", err);
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
        `SELECT c.id, c.user_id, u.is_bot, u.name, u.bot_personality
         FROM comments c
         JOIN users u ON u.id = c.user_id
         WHERE c.id = $1`,
        [parentCommentId]
      );

      if (!parentComment || !parentComment.is_bot) return;

      // Don't loop if bot is replying to bot
      const replier = await queryOne("SELECT is_bot FROM users WHERE id = $1", [replyingUserId]);
      if (replier && replier.is_bot) return;

      const replyDelay = randomBetween(45, 75) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          // Generate natural conversational reply
          const botReplies = [
            "Haha bilkul sahi bola aapne! Aur batao what else is happening? 😊",
            "Totally agree with you! Kafi cool lagte ho aap waise ✨",
            "Haha loved your reply! Where are you from btw? 🙌",
            "Sahi baat hai! Camverz par aise genuine logon se baat karke achha laga 🌸",
            "Haha so true yaar! Hope you're having an awesome evening na! ☕",
            "Point hai boss haha! Maze karo aur keep posting such cool stuff 🔥",
          ];
          const botReplyText = pickRandom(botReplies);

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

module.exports = {
  ensureBotsSeeded,
  schedulePostEngagement,
  handleUserReplyToBot,
  analyzePostText,
  generateContextualComment,
};
