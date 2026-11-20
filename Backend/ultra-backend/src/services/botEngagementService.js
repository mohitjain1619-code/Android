const http = require("http");
const https = require("https");
const { query, queryOne, queryMany } = require("../config/database");
const NotificationService = require("./notificationService");

/**
 * =========================================================================
 * BOT ENGAGEMENT & RETENTION SERVICE (LEVEL 1 & LEVEL 2 RETENTION BOOSTER)
 * =========================================================================
 * - 50+ Diverse Ultra-Realistic Indian & Global Bot Profiles
 * - Native Avatars Only (Girls: av3, av4, av5, av10, av11, av12, av13 | Boys: av2, av6, av7, av9, av14, av15)
 * - Strict Sexual Preference & Orientation Alignment (Gay, Lesbian, Straight)
 * - Level 1: Automatic Staggered Post Engagement (Likes + AI Comments)
 * - Level 1: Automated Story Viewers Engine (6-12 realistic viewers)
 * - Level 1: 2-Way Interactive Comment Threading (Bot replies to user comments)
 * - Level 2: Instant Welcome / Hook DMs (Personalized opener in 45-80s)
 * - Level 2: AI-Powered Multi-Turn Natural DM Chat (Groq Cloud Llama 3.1 + Local Ollama Llama 3.2 — ₹0 Cost)
 * - Level 2: Real-time "Typing..." Socket Indicator (Realistic 3.5-7s human delay)
 * - Level 2: Conversational Memory (Maintains 8-10 message context arc)
 * - Level 2: RealMeet Video Call Pitch (Naturally invites user to video call after rapport is built)
 * =========================================================================
 */

// 50+ Diverse Ultra-Realistic Bot Profiles (Native App Avatars Only)
const BOT_PROFILES = [
  // --- FEMALE PROFILES (32 Profiles) ---
  {
    google_id: "bot_priya_sharma",
    email: "priya.bot@camverz.internal",
    name: "Priya Sharma",
    gender: "female",
    avatar: "av3",
    photo_url: null,
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
    photo_url: null,
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
    avatar: "av5",
    photo_url: null,
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
    avatar: "av10",
    photo_url: null,
    bio: "Simple living, high thinking 🌸 New here!",
    city: "Ahmedabad",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_simran_kaur",
    email: "simran.bot@camverz.internal",
    name: "Simran Kaur",
    gender: "female",
    avatar: "av11",
    photo_url: null,
    bio: "Dance, fashion & iced latte ☕✨ Let's connect!",
    city: "Chandigarh",
    verified: true,
    personality: "witty_genz",
  },
  {
    google_id: "bot_natasha_dsouza",
    email: "natasha.bot@camverz.internal",
    name: "Natasha D'Souza",
    gender: "female",
    avatar: "av12",
    photo_url: null,
    bio: "Beach lover 🌊 Music festival junkie | Free to chat",
    city: "Goa",
    verified: false,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_kritika_sharma",
    email: "kritika.bot@camverz.internal",
    name: "Kritika Sharma",
    gender: "female",
    avatar: "av13",
    photo_url: null,
    bio: "Law student | Late night talks are the best 🌙",
    city: "Delhi",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_alisha_khan",
    email: "alisha.bot@camverz.internal",
    name: "Alisha Khan",
    gender: "female",
    avatar: "av3",
    photo_url: null,
    bio: "Design, aesthetics & sunsets 🌇 Open to video calls",
    city: "Mumbai",
    verified: true,
    personality: "flirty_charming",
  },
  {
    google_id: "bot_pooja_nair",
    email: "pooja.bot@camverz.internal",
    name: "Pooja Nair",
    gender: "female",
    avatar: "av4",
    photo_url: null,
    bio: "Always curious | Looking for genuine friendships 🌸",
    city: "Kochi",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_megha_singhania",
    email: "megha.bot@camverz.internal",
    name: "Megha Singhania",
    gender: "female",
    avatar: "av5",
    photo_url: null,
    bio: "Bookworm & pizza enthusiast 🍕",
    city: "Kolkata",
    verified: false,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_zara_sheikh",
    email: "zara.bot@camverz.internal",
    name: "Zara Sheikh",
    gender: "female",
    avatar: "av10",
    photo_url: null,
    bio: "Hyderabad vibes | Biryani & Bollywood lover ❤️",
    city: "Hyderabad",
    verified: true,
    personality: "flirty_charming",
  },
  {
    google_id: "bot_shreya_joshi",
    email: "shreya.bot@camverz.internal",
    name: "Shreya Joshi",
    gender: "female",
    avatar: "av11",
    photo_url: null,
    bio: "Wanderlust | Tea over coffee always 🍵",
    city: "Pune",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_tanya_kapoor",
    email: "tanya.bot@camverz.internal",
    name: "Tanya Kapoor",
    gender: "female",
    avatar: "av12",
    photo_url: null,
    bio: "Living one day at a time 💫 Let's connect",
    city: "Jaipur",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_divya_menon",
    email: "divya.bot@camverz.internal",
    name: "Divya Menon",
    gender: "female",
    avatar: "av13",
    photo_url: null,
    bio: "Techie by day, gamer by night 🎮 Let's talk!",
    city: "Chennai",
    verified: false,
    personality: "witty_genz",
  },
  {
    google_id: "bot_ishita_roy",
    email: "ishita.bot@camverz.internal",
    name: "Ishita Roy",
    gender: "female",
    avatar: "av3",
    photo_url: null,
    bio: "Art, poetry & good conversations 🎨 Kolkata",
    city: "Kolkata",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_tanvi_gupta",
    email: "tanvi.bot@camverz.internal",
    name: "Tanvi Gupta",
    gender: "female",
    avatar: "av4",
    photo_url: null,
    bio: "Life is short, smile while you still have teeth 😁 Lucknow",
    city: "Lucknow",
    verified: false,
    personality: "witty_genz",
  },
  {
    google_id: "bot_avni_deshmukh",
    email: "avni.bot@camverz.internal",
    name: "Avni Deshmukh",
    gender: "female",
    avatar: "av5",
    photo_url: null,
    bio: "Classic rock & road trips 🚗 Nagpur",
    city: "Nagpur",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_khushi_mehra",
    email: "khushi.bot@camverz.internal",
    name: "Khushi Mehra",
    gender: "female",
    avatar: "av10",
    photo_url: null,
    bio: "Sunshine mixed with a little hurricane 🌪️ Delhi",
    city: "Delhi",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_neha_choudhary",
    email: "neha.bot@camverz.internal",
    name: "Neha Choudhary",
    gender: "female",
    avatar: "av11",
    photo_url: null,
    bio: "MBA student | Coffee addict ☕ Gurgaon",
    city: "Gurgaon",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_sakshi_malik",
    email: "sakshi.bot@camverz.internal",
    name: "Sakshi Malik",
    gender: "female",
    avatar: "av12",
    photo_url: null,
    bio: "Fitness & healthy lifestyle 🥗 Noida",
    city: "Noida",
    verified: false,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_rhea_fernandes",
    email: "rhea.bot@camverz.internal",
    name: "Rhea Fernandes",
    gender: "female",
    avatar: "av13",
    photo_url: null,
    bio: "Sunset chaser & beach soul 🌅 Goa",
    city: "Goa",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_aditi_bhatia",
    email: "aditi.bot@camverz.internal",
    name: "Aditi Bhatia",
    gender: "female",
    avatar: "av3",
    photo_url: null,
    bio: "Passionate about photography & stories 📸 Indore",
    city: "Indore",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_chloe_miller",
    email: "chloe.bot@camverz.internal",
    name: "Chloe Miller",
    gender: "female",
    avatar: "av4",
    photo_url: null,
    bio: "Living in New York 🗽 Traveling the world & loving it!",
    city: "New York",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_emma_davis",
    email: "emma.bot@camverz.internal",
    name: "Emma Davis",
    gender: "female",
    avatar: "av5",
    photo_url: null,
    bio: "Art, coffee & cozy evenings ☕ London vibes",
    city: "London",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_sophia_wilson",
    email: "sophia.bot@camverz.internal",
    name: "Sophia Wilson",
    gender: "female",
    avatar: "av10",
    photo_url: null,
    bio: "Music producer & DJ 🎧 Toronto",
    city: "Toronto",
    verified: true,
    personality: "witty_genz",
  },
  {
    google_id: "bot_mia_taylor",
    email: "mia.bot@camverz.internal",
    name: "Mia Taylor",
    gender: "female",
    avatar: "av11",
    photo_url: null,
    bio: "Fashion student & vintage lover 👗 London",
    city: "London",
    verified: false,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_payal_shukla",
    email: "payal.bot@camverz.internal",
    name: "Payal Shukla",
    gender: "female",
    avatar: "av12",
    photo_url: null,
    bio: "Architecture & chai ☕ Bhopal",
    city: "Bhopal",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_vidya_balan",
    email: "vidya.bot@camverz.internal",
    name: "Vidya Raghavan",
    gender: "female",
    avatar: "av13",
    photo_url: null,
    bio: "Music & techie life 🎵 Bangalore",
    city: "Bangalore",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_anushka_sen",
    email: "anushka.bot@camverz.internal",
    name: "Anushka Sen",
    gender: "female",
    avatar: "av3",
    photo_url: null,
    bio: "Smiles are free therapy ✨ Delhi",
    city: "Delhi",
    verified: true,
    personality: "flirty_charming",
  },
  {
    google_id: "bot_kajal_agarwal",
    email: "kajal.bot@camverz.internal",
    name: "Kajal Agarwal",
    gender: "female",
    avatar: "av4",
    photo_url: null,
    bio: "Cinema lover & foodie 🍕 Chandigarh",
    city: "Chandigarh",
    verified: false,
    personality: "witty_genz",
  },
  {
    google_id: "bot_deepika_rao",
    email: "deepika.bot@camverz.internal",
    name: "Deepika Rao",
    gender: "female",
    avatar: "av5",
    photo_url: null,
    bio: "Fitness, wellness & positive vibes 🧘‍♀️ Mysore",
    city: "Mysore",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_sonam_kapoor",
    email: "sonam.bot@camverz.internal",
    name: "Sonam Kapoor",
    gender: "female",
    avatar: "av10",
    photo_url: null,
    bio: "Stylist & aesthetic content creator 🌸 Mumbai",
    city: "Mumbai",
    verified: true,
    personality: "flirty_charming",
  },

  // --- MALE PROFILES (18 Profiles) ---
  {
    google_id: "bot_kabir_mehta",
    email: "kabir.bot@camverz.internal",
    name: "Kabir Mehta",
    gender: "male",
    avatar: "av2",
    photo_url: null,
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
    photo_url: null,
    bio: "Chill vibes only ✌️ Music producer at night",
    city: "Chandigarh",
    verified: false,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_rohan_malhotra",
    email: "rohan.bot@camverz.internal",
    name: "Rohan Malhotra",
    gender: "male",
    avatar: "av7",
    photo_url: null,
    bio: "Coffee addict & weekend trekker ⛰️",
    city: "Bangalore",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_siddharth_rao",
    email: "sid.bot@camverz.internal",
    name: "Siddharth Rao",
    gender: "male",
    avatar: "av9",
    photo_url: null,
    bio: "Startup life | Weekend traveler ✈️ Let's connect",
    city: "Bangalore",
    verified: true,
    personality: "bro_hype",
  },
  {
    google_id: "bot_varun_kapoor",
    email: "varun.bot@camverz.internal",
    name: "Varun Kapoor",
    gender: "male",
    avatar: "av14",
    photo_url: null,
    bio: "Cricket & photography 📸 Exploring new cities",
    city: "Mumbai",
    verified: true,
    personality: "flirty_charming",
  },
  {
    google_id: "bot_sameer_khan",
    email: "sameer.bot@camverz.internal",
    name: "Sameer Khan",
    gender: "male",
    avatar: "av15",
    photo_url: null,
    bio: "Fitness, cars & late night drives 🏎️",
    city: "Delhi",
    verified: false,
    personality: "bro_hype",
  },
  {
    google_id: "bot_armaan_malik",
    email: "armaan.bot@camverz.internal",
    name: "Armaan Malik",
    gender: "male",
    avatar: "av2",
    photo_url: null,
    bio: "Guitarist & chill vibes 🎸 Hit me up on call",
    city: "Jaipur",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_lucas_gray",
    email: "lucas.bot@camverz.internal",
    name: "Lucas Gray",
    gender: "male",
    avatar: "av6",
    photo_url: null,
    bio: "Surfing, design & good music 🎶 California",
    city: "Los Angeles",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_karan_singh",
    email: "karan.bot@camverz.internal",
    name: "Karan Singh",
    gender: "male",
    avatar: "av7",
    photo_url: null,
    bio: "Gym rat | Protein & hustle 💪 Gurgaon",
    city: "Gurgaon",
    verified: true,
    personality: "bro_hype",
  },
  {
    google_id: "bot_aman_jain",
    email: "aman.bot@camverz.internal",
    name: "Aman Jain",
    gender: "male",
    avatar: "av9",
    photo_url: null,
    bio: "CA by day, traveler on weekends ✈️ Ahmedabad",
    city: "Ahmedabad",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_aditya_sharma",
    email: "aditya.bot@camverz.internal",
    name: "Aditya Sharma",
    gender: "male",
    avatar: "av14",
    photo_url: null,
    bio: "Software developer & gamer 🎮 Hyderabad",
    city: "Hyderabad",
    verified: false,
    personality: "witty_genz",
  },
  {
    google_id: "bot_vikram_rathore",
    email: "vikram.bot@camverz.internal",
    name: "Vikram Rathore",
    gender: "male",
    avatar: "av15",
    photo_url: null,
    bio: "Royal Enfield rider & mountain trekker 🏍️ Jaipur",
    city: "Jaipur",
    verified: true,
    personality: "chill_relaxed",
  },
  {
    google_id: "bot_harsh_vardhan",
    email: "harsh.bot@camverz.internal",
    name: "Harsh Vardhan",
    gender: "male",
    avatar: "av2",
    photo_url: null,
    bio: "Food blogger & street food hunter 🍲 Delhi",
    city: "Delhi",
    verified: true,
    personality: "witty_genz",
  },
  {
    google_id: "bot_neil_desouza",
    email: "neil.bot@camverz.internal",
    name: "Neil D'Souza",
    gender: "male",
    avatar: "av6",
    photo_url: null,
    bio: "Guitarist & singer 🎤 Mumbai",
    city: "Mumbai",
    verified: false,
    personality: "flirty_charming",
  },
  {
    google_id: "bot_noah_clark",
    email: "noah.bot@camverz.internal",
    name: "Noah Clark",
    gender: "male",
    avatar: "av7",
    photo_url: null,
    bio: "Photographer & filmmaker 🎥 London",
    city: "London",
    verified: true,
    personality: "curious_thoughtful",
  },
  {
    google_id: "bot_liam_smith",
    email: "liam.bot@camverz.internal",
    name: "Liam Smith",
    gender: "male",
    avatar: "av9",
    photo_url: null,
    bio: "Fitness trainer & nutritionist 🥑 Sydney",
    city: "Sydney",
    verified: true,
    personality: "bro_hype",
  },
  {
    google_id: "bot_rahul_verma",
    email: "rahul.bot@camverz.internal",
    name: "Rahul Verma",
    gender: "male",
    avatar: "av14",
    photo_url: null,
    bio: "Tech entrepreneur | Coffee enthusiast ☕ Pune",
    city: "Pune",
    verified: true,
    personality: "friendly_sweet",
  },
  {
    google_id: "bot_ayush_sharma",
    email: "ayush.bot@camverz.internal",
    name: "Ayush Sharma",
    gender: "male",
    avatar: "av15",
    photo_url: null,
    bio: "Night drives & lo-fi beats 🎵 Chandigarh",
    city: "Chandigarh",
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
      `SELECT id, name, gender, avatar, photo_url, bot_personality, city
       FROM users ${genderFilter}
       ORDER BY RANDOM() LIMIT $1`,
      [count]
    );

    if (bots && bots.length > 0) return bots;

    // Fallback if specific filter has no bots
    return await queryMany(
      "SELECT id, name, gender, avatar, photo_url, bot_personality, city FROM users WHERE is_bot = true ORDER BY RANDOM() LIMIT $1",
      [count]
    );
  } catch (err) {
    console.error("Error fetching matching bots:", err);
    return [];
  }
}

/**
 * =========================================================================
 * HYBRID AI ENGINE: GROQ CLOUD (FREE LLAMA 3.1) + LOCAL OLLAMA (LLAMA 3.2)
 * Supports multi-turn chat message history for realistic conversational memory.
 * =========================================================================
 */
function callGroqAI(systemPrompt, userPrompt, messagesHistory = [], timeoutMs = 8000) {
  const apiKey = process.env.GROQ_API_KEY;
  if (!apiKey || !apiKey.trim()) return Promise.resolve(null);

  return new Promise((resolve) => {
    let resolved = false;
    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true;
        resolve(null);
      }
    }, timeoutMs);

    try {
      // Build multi-turn messages array
      const messages = [{ role: "system", content: systemPrompt }];
      if (Array.isArray(messagesHistory) && messagesHistory.length > 0) {
        messages.push(...messagesHistory);
      }
      if (userPrompt && userPrompt.trim()) {
        messages.push({ role: "user", content: userPrompt });
      }

      const payload = JSON.stringify({
        model: "llama-3.1-8b-instant",
        messages,
        temperature: 0.8,
        max_tokens: 60,
      });

      const req = https.request(
        {
          hostname: "api.groq.com",
          port: 443,
          path: "/openai/v1/chat/completions",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${apiKey.trim()}`,
            "Content-Length": Buffer.byteLength(payload),
          },
        },
        (res) => {
          let body = "";
          res.on("data", (chunk) => (body += chunk));
          res.on("end", () => {
            if (!resolved) {
              resolved = true;
              clearTimeout(timer);
              try {
                const json = JSON.parse(body);
                if (json && json.choices && json.choices[0] && json.choices[0].message) {
                  let cleaned = json.choices[0].message.content.trim();
                  cleaned = cleanAIResponse(cleaned);
                  if (cleaned.length >= 3) {
                    return resolve(cleaned);
                  }
                }
              } catch (e) {}
              resolve(null);
            }
          });
        }
      );

      req.on("error", () => {
        if (!resolved) {
          resolved = true;
          clearTimeout(timer);
          resolve(null);
        }
      });

      req.write(payload);
      req.end();
    } catch (err) {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        resolve(null);
      }
    }
  });
}

function callLocalOllamaAI(systemPrompt, userPrompt, messagesHistory = [], timeoutMs = 25000) {
  return new Promise((resolve) => {
    let resolved = false;
    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true;
        resolve(null);
      }
    }, timeoutMs);

    try {
      const messages = [{ role: "system", content: systemPrompt }];
      if (Array.isArray(messagesHistory) && messagesHistory.length > 0) {
        messages.push(...messagesHistory);
      }
      if (userPrompt && userPrompt.trim()) {
        messages.push({ role: "user", content: userPrompt });
      }

      const payload = JSON.stringify({
        model: process.env.OLLAMA_MODEL || "llama3.2:latest",
        messages,
        stream: false,
        options: {
          temperature: 0.8,
          num_predict: 50,
        },
      });

      const req = http.request(
        {
          hostname: "127.0.0.1",
          port: 11434,
          path: "/api/chat",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Content-Length": Buffer.byteLength(payload),
          },
        },
        (res) => {
          let body = "";
          res.on("data", (chunk) => (body += chunk));
          res.on("end", () => {
            if (!resolved) {
              resolved = true;
              clearTimeout(timer);
              try {
                const json = JSON.parse(body);
                if (json && json.message && json.message.content) {
                  let cleaned = cleanAIResponse(json.message.content.trim());
                  if (cleaned.length >= 3) {
                    return resolve(cleaned);
                  }
                }
              } catch (e) {}
              resolve(null);
            }
          });
        }
      );

      req.on("error", () => {
        if (!resolved) {
          resolved = true;
          clearTimeout(timer);
          resolve(null);
        }
      });

      req.write(payload);
      req.end();
    } catch (err) {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        resolve(null);
      }
    }
  });
}

// Clean raw LLM output of unwanted formatting, markdown quotes or prefixes
function cleanAIResponse(text) {
  if (!text) return "";
  let cleaned = text.trim();
  cleaned = cleaned.replace(/^["'`]+|["'`]+$/g, "");
  cleaned = cleaned.replace(/^[A-Za-z0-9_]+:\s*/, "");
  cleaned = cleaned.replace(/^(Comment|Reply|Message|Bot):\s*/i, "");
  cleaned = cleaned.replace(/\n.*/gs, ""); // Keep only first line
  return cleaned.trim();
}

/**
 * =========================================================================
 * POST CONTEXT NLP ANALYZER
 * =========================================================================
 */
function analyzePostText(rawText) {
  const text = (rawText || "").toLowerCase().trim();
  const hasQuestion = /[?]|kya|kaun|kahan|kaise|which|suggest|recommend|anyone|tell me|opinion/i.test(text);

  return {
    rawText,
    hasQuestion,
  };
}

/**
 * Dynamic Comment Generator using Hybrid AI Engine
 */
async function generateContextualComment(analysis, botPersonality, botName, isIndianUser = true, preferHinglish = false, userGender = "male", userPreference = "straight", botGender = "female") {
  const { rawText } = analysis || {};

  const personaDesc = botGender === "female" ? "charming, sweet, modern 21yo Indian girl" : "cool, chill, friendly 22yo Indian guy";
  const targetDesc = userPreference === "gay" ? "a gay guy" : (userPreference === "lesbian" ? "a lesbian girl" : (userGender === "male" ? "a guy" : "a girl"));
  const langDesc = preferHinglish
    ? "natural casual conversational Hinglish (blend of English & Hindi in English script, e.g. 'Haha bilkul sahi bola', 'Bilkul mast')"
    : "clean, friendly, natural conversational English";

  const systemPrompt = `You are ${botName}, a ${personaDesc} on a social media app called Camverz. Personality: ${botPersonality || "friendly"}.
You are commenting on a post made by ${targetDesc}.
STRICT INSTRUCTIONS:
1. React specifically and naturally to what they wrote in their post. Show that you read it.
2. Language: ${langDesc}.
3. Length: Exactly 1 single short sentence (8 to 15 words).
4. Add 1 or 2 matching emojis.
5. Do NOT include quotation marks, explanations, or your name. Output ONLY the comment text.`;

  const userPrompt = `Post content: "${(rawText || "").trim()}"`;

  // 1. Try Groq Cloud first (Free Llama 3.1)
  try {
    const groqRes = await callGroqAI(systemPrompt, userPrompt);
    if (groqRes) return groqRes;
  } catch (e) {}

  // 2. Try Local Ollama (Llama 3.2)
  try {
    const ollamaRes = await callLocalOllamaAI(systemPrompt, userPrompt);
    if (ollamaRes) return ollamaRes;
  } catch (e) {}

  // 3. Fallback
  if (preferHinglish) {
    return pickRandom([
      "Haha bilkul sahi bola aapne! So true ✨",
      "Loved this post! Really nice vibe 🙌",
      "Haha totally agree with you on this! 🌸",
      "Sahi baat hai yaar, keep sharing such cool stuff 🔥",
      "Nice one! Hope you're having an awesome day 😊",
    ]);
  }
  return pickRandom([
    "Haha totally agree with this! Such great vibes ✨",
    "Loved your post! Hope you have an awesome day 😊",
    "Haha this made me smile, so true 🙌",
    "Great perspective honestly! Keep sharing 🌸",
    "Totally on point! Loved reading this 🔥",
  ]);
}

/**
 * =========================================================================
 * SCHEDULE AUTOMATED POST ENGAGEMENT (MULTI-WAVE ENGAGEMENT OVER 10 MINS)
 * =========================================================================
 */
function schedulePostEngagement(postId, authorUserId, postText, category, userGender, sexPreference) {
  setImmediate(async () => {
    try {
      const author = await queryOne("SELECT is_bot, country, gender, sex_preference FROM users WHERE id = $1", [authorUserId]);
      if (author && author.is_bot) return;

      const userGen = (author && author.gender ? author.gender : userGender || "male").toLowerCase();
      const userPref = (author && author.sex_preference ? author.sex_preference : sexPreference || "straight").toLowerCase();
      const isIndianUser = !author || !author.country || author.country.toLowerCase() === "india";

      const analysis = analyzePostText(postText);
      const bots = await getMatchingBots(userGen, userPref, 5);
      if (!bots || bots.length === 0) return;

      const bot1 = bots[0];
      const bot2 = bots.length > 1 ? bots[1] : null;
      const bot3 = bots.length > 2 ? bots[2] : null;
      const bot4 = bots.length > 3 && Math.random() < 0.85 ? bots[3] : null;
      const bot5 = bots.length > 4 && Math.random() < 0.65 ? bots[4] : null;

      // BOT 1: Like in 20-35s, Comment in 35-65s
      const bot1UsesHinglish = isIndianUser && Math.random() < 0.5;
      const bot1LikeDelay = randomBetween(20, 35) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot1.id]);
          await query(
            `INSERT INTO notifications (user_id, type, triggering_user_id, post_id) VALUES ($1, 'like', $2, $3)`,
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
            `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id) VALUES ($1, 'comment', $2, $3, $4)`,
            [authorUserId, bot1.id, postId, comment.id]
          );

          NotificationService.sendToUser(authorUserId, {
            title: "New Comment 💬",
            body: `${bot1.name}: ${commentText.substring(0, 80)}`,
            type: "comment",
            channelId: "camverz_social",
            data: { postId, commentId: comment.id, triggeringUserId: bot1.id },
          });
        } catch (e) {
          console.error("Bot1 comment error:", e.message);
        }
      }, bot1CommentDelay);

      // BOT 2: Like in 75-110s, Comment in 95-140s
      if (bot2) {
        const bot2UsesHinglish = isIndianUser && !bot1UsesHinglish;
        const bot2LikeDelay = randomBetween(75, 110) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot2.id]);
            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id) VALUES ($1, 'like', $2, $3)`,
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
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id) VALUES ($1, 'comment', $2, $3, $4)`,
              [authorUserId, bot2.id, postId, comment.id]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Comment 💬",
              body: `${bot2.name}: ${commentText.substring(0, 80)}`,
              type: "comment",
              channelId: "camverz_social",
              data: { postId, commentId: comment.id, triggeringUserId: bot2.id },
            });
          } catch (e) {
            console.error("Bot2 comment error:", e.message);
          }
        }, bot2CommentDelay);
      }

      // BOT 3: Like in 160-220s, Comment in 190-260s
      if (bot3) {
        const bot3UsesHinglish = isIndianUser && Math.random() < 0.5;
        const bot3LikeDelay = randomBetween(160, 220) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            await query("INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", [postId, bot3.id]);
            await query(
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id) VALUES ($1, 'like', $2, $3)`,
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
              `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id) VALUES ($1, 'comment', $2, $3, $4)`,
              [authorUserId, bot3.id, postId, comment.id]
            );

            NotificationService.sendToUser(authorUserId, {
              title: "New Comment 💬",
              body: `${bot3.name}: ${commentText.substring(0, 80)}`,
              type: "comment",
              channelId: "camverz_social",
              data: { postId, commentId: comment.id, triggeringUserId: bot3.id },
            });
          } catch (e) {
            console.error("Bot3 comment error:", e.message);
          }
        }, bot3CommentDelay);
      }
    } catch (err) {
      console.error("Post engagement scheduler error:", err);
    }
  });
}

/**
 * =========================================================================
 * SCHEDULE AUTOMATED STORY VIEWS (LEVEL 1 STORY ENGAGEMENT)
 * Staggers 6 to 12 realistic story views across 5-25 minutes!
 * =========================================================================
 */
function scheduleStoryViews(storyId, authorUserId) {
  setImmediate(async () => {
    try {
      const author = await queryOne("SELECT is_bot, gender, sex_preference FROM users WHERE id = $1", [authorUserId]);
      if (author && author.is_bot) return;

      const userGen = (author && author.gender ? author.gender : "male").toLowerCase();
      const userPref = (author && author.sex_preference ? author.sex_preference : "straight").toLowerCase();

      const viewerCount = randomBetween(6, 12);
      const bots = await getMatchingBots(userGen, userPref, viewerCount);
      if (!bots || bots.length === 0) return;

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

      const parentComment = await queryOne(
        `SELECT c.id, c.user_id, u.is_bot, u.name, u.bot_personality, u.gender
         FROM comments c
         JOIN users u ON u.id = c.user_id
         WHERE c.id = $1`,
        [parentCommentId]
      );

      if (!parentComment || !parentComment.is_bot) return;

      const replier = await queryOne("SELECT is_bot, country, gender, sex_preference FROM users WHERE id = $1", [replyingUserId]);
      if (replier && replier.is_bot) return;

      const isIndianUser = !replier || !replier.country || replier.country.toLowerCase() === "india";
      const replyInHinglish = isIndianUser && Math.random() < 0.5;

      const replyDelay = randomBetween(45, 75) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

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

          // 1. Try Groq Cloud first
          try {
            botReplyText = await callGroqAI(replySystem, `User replied to your comment: "${userReplyText}"`);
          } catch (e) {}

          // 2. Try Local Ollama
          if (!botReplyText) {
            try {
              botReplyText = await callLocalOllamaAI(replySystem, `User replied to your comment: "${userReplyText}"`);
            } catch (e) {}
          }

          if (!botReplyText) {
            botReplyText = replyInHinglish
              ? pickRandom([
                  "Haha bilkul sahi bola aapne! Aur batao what else is happening? 😊",
                  "Totally agree with you! Kafi cool lagte ho aap waise ✨",
                  "Haha loved your reply! Where are you from btw? 🙌",
                  "Sahi baat hai! Camverz par aise genuine logon se baat karke achha laga 🌸",
                  "Free ho to kabhi RealMeet par video call karte hain, what say? 😉✨",
                ])
              : pickRandom([
                  "Haha totally agree with you! How's the rest of your day going? 😊",
                  "Love your perspective honestly! Such cool vibes ✨",
                  "Haha loved your reply! Where are you from originally? 🙌",
                  "You seem super fun, we should totally video chat on RealMeet sometime! 😉",
                ]);
          }

          const inserted = await queryOne(
            `INSERT INTO comments (post_id, user_id, text, parent_id) VALUES ($1, $2, $3, $4) RETURNING *`,
            [postId, parentComment.user_id, botReplyText, parentCommentId]
          );

          await query(
            `INSERT INTO notifications (user_id, type, triggering_user_id, post_id, comment_id) VALUES ($1, 'reply', $2, $3, $4)`,
            [replyingUserId, parentComment.user_id, postId, inserted.id]
          );

          NotificationService.sendToUser(replyingUserId, {
            title: "New Reply 💬",
            body: `${parentComment.name}: ${botReplyText.substring(0, 80)}`,
            type: "reply",
            channelId: "camverz_social",
            data: { postId, commentId: inserted.id, triggeringUserId: parentComment.user_id },
          });
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
 * LEVEL 2: INSTANT WELCOME / HOOK DMS (RETENTION BOOSTER)
 * 1-2 minutes after signup or active browsing, a matching bot sends a hook DM.
 * Strictly 1 welcome DM per user ever!
 * =========================================================================
 */
// In-memory set to prevent duplicate scheduling in concurrent calls
const scheduledWelcomeUsers = new Set();

function scheduleWelcomeDM(userId, appIo = null, appRedis = null) {
  if (!userId || scheduledWelcomeUsers.has(userId)) return;
  scheduledWelcomeUsers.add(userId);

  setImmediate(async () => {
    try {
      const user = await queryOne("SELECT id, name, gender, sex_preference, country, is_bot FROM users WHERE id = $1", [userId]);
      if (!user || user.is_bot) return;

      // Check if user already has ANY conversation or message from a bot
      const existingBotChat = await queryOne(
        `SELECT 1 FROM messages m
         JOIN chats c ON c.id = m.chat_id
         JOIN users u ON u.id = m.sender_id
         WHERE u.is_bot = true AND (c.user1_id = $1 OR c.user2_id = $1)
         LIMIT 1`,
        [userId]
      );
      if (existingBotChat) return;

      // Select 1 matching bot strictly aligned with user preference & gender
      const matchingBots = await getMatchingBots(user.gender, user.sex_preference, 1);
      if (!matchingBots || matchingBots.length === 0) return;
      const bot = matchingBots[0];

      // Delay welcome message naturally: 45 to 80 seconds
      const welcomeDelay = randomBetween(45, 80) * 1000;
      console.log(`💌 Scheduling Level 2 Welcome DM from ${bot.name} to user ${user.name || user.id} in ${Math.round(welcomeDelay / 1000)}s`);

      setTimeout(async () => {
        try {
          // Double check user still exists and hasn't received a bot message in the interim
          const secondCheck = await queryOne(
            `SELECT 1 FROM messages m
             JOIN chats c ON c.id = m.chat_id
             JOIN users u ON u.id = m.sender_id
             WHERE u.is_bot = true AND (c.user1_id = $1 OR c.user2_id = $1)
             LIMIT 1`,
            [userId]
          );
          if (secondCheck) return;

          const isIndianUser = !user.country || user.country.toLowerCase() === "india";
          const useHinglish = isIndianUser;

          // Select natural, high-retention hook
          let hookText = "";
          if (bot.gender === "female") {
            // Female bot messaging male / lesbian user
            if (useHinglish) {
              const hooks = [
                "Hey! Just saw your profile, where are you from? 😊",
                "Heyy! Love your vibe on here ✨ free ho abhi baat karne ke liye?",
                "Hey! Just joined Camverz, kaise ho aap? 🌸",
                "Hey there! Saw your profile on feed, looking good! Where are you from? ✨",
                "Hey! Online ho abhi? Free ho to let's chat 😊",
              ];
              hookText = pickRandom(hooks);
            } else {
              const hooks = [
                "Hey! Just came across your profile, where are you from? 😊",
                "Hey there! Love your vibe on here ✨ how's your day going?",
                "Hey! Just joined Camverz, what are you up to today? 🌸",
                "Hey handsome, are you online? 😊",
              ];
              hookText = pickRandom(hooks);
            }
          } else {
            // Male bot messaging female / gay user
            if (useHinglish) {
              const hooks = [
                "Hey! Just saw your profile here, couldn't resist saying hi 😊 where are you from?",
                "Hey! Love your vibe on here ✨ how are you doing today?",
                "Hey there! Kaisa chal raha hai aapka day? 😊",
                "Hey! Saw your profile on feed, really cool photos ✨ where are you from?",
              ];
              hookText = pickRandom(hooks);
            } else {
              const hooks = [
                "Hey! Just saw your profile here, couldn't resist saying hi 😊 where are you from?",
                "Hey there! Love your vibe ✨ hope you're having a good day!",
                "Hey! Just joined Camverz, where are you from? 😊",
              ];
              hookText = pickRandom(hooks);
            }
          }

          // Find or create chat
          const [u1, u2] = [userId, bot.id].sort();
          let chat = await queryOne("SELECT * FROM chats WHERE user1_id = $1 AND user2_id = $2", [u1, u2]);
          if (!chat) {
            chat = await queryOne("INSERT INTO chats (user1_id, user2_id) VALUES ($1, $2) RETURNING *", [u1, u2]);
          }

          // Unhide chat for both users if previously hidden
          await query("UPDATE chats SET user1_deleted = false, user2_deleted = false, last_message_at = NOW() WHERE id = $1", [chat.id]);

          // Insert welcome message
          const msg = await queryOne(
            `INSERT INTO messages (chat_id, sender_id, text, read) VALUES ($1, $2, $3, false) RETURNING *`,
            [chat.id, bot.id, hookText]
          );

          // Deliver real-time socket event
          const io = appIo || global.__io;
          const redisModule = appRedis || global.__redisModule;
          if (io && redisModule) {
            const userSocket = await redisModule.getUserSocket(userId);
            if (userSocket) {
              io.to(userSocket).emit("new_message", {
                senderId: bot.id,
                chatId: chat.id,
                text: hookText,
                messageId: msg.id,
                createdAt: msg.created_at,
                replyToId: null,
                replyToText: null,
                replyToSenderName: null,
              });
            }
          }

          // Send high-priority FCM Push Notification to wake up phone screen!
          NotificationService.sendToUser(userId, {
            title: bot.name,
            body: hookText,
            type: "chat",
            channelId: "camverz_chats",
            highPriority: true,
            data: {
              targetUserId: bot.id,
              targetUserName: bot.name,
              targetUserAvatar: bot.avatar || "",
              chatId: chat.id,
              messageId: msg.id,
            },
          });

          console.log(`✅ Level 2 Welcome DM delivered to user ${userId} from bot ${bot.name}: "${hookText}"`);
        } catch (err) {
          console.error("Error executing welcome DM delivery:", err);
        }
      }, welcomeDelay);
    } catch (e) {
      console.error("Error in scheduleWelcomeDM:", e);
    }
  });
}

/**
 * =========================================================================
 * LEVEL 2: AI-POWERED NATURAL CHAT INCOMING HANDLER
 * Handles user reply to any bot in DM:
 * - Emits real-time typing indicator after 1.5s ("Priya is typing...")
 * - Reads last 8-10 messages for conversation context & memory
 * - Replies after 3.5 to 7.0s natural human delay
 * - After 3-4 messages, naturally invites user to a RealMeet video call!
 * =========================================================================
 */
function handleIncomingDMToBot({ chatId, botUser, realUserId, incomingText, io, redisModule }) {
  setImmediate(async () => {
    try {
      if (!chatId || !botUser || !realUserId || !incomingText) return;

      const user = await queryOne("SELECT id, name, gender, sex_preference, country FROM users WHERE id = $1", [realUserId]);
      if (!user) return;

      const isIndianUser = !user.country || user.country.toLowerCase() === "india";

      // 1. Send realistic "Typing..." socket indicator after 1.2 to 2.0s
      const typingStartDelay = randomBetween(1200, 2000);
      const totalReplyDelay = randomBetween(4200, 7200);

      setTimeout(async () => {
        try {
          const activeIo = io || global.__io;
          const activeRedis = redisModule || global.__redisModule;
          if (activeIo && activeRedis) {
            const userSocket = await activeRedis.getUserSocket(realUserId);
            if (userSocket) {
              activeIo.to(userSocket).emit("user_typing", {
                senderId: botUser.id,
                senderName: botUser.name,
                chatId: chatId,
                isTyping: true,
              });
            }
          }
        } catch (e) {
          console.error("Typing start indicator error:", e.message);
        }
      }, typingStartDelay);

      // 2. Fetch conversation history (last 10 messages) for conversational memory
      const recentMessages = await queryMany(
        `SELECT m.sender_id, m.text, m.created_at
         FROM messages m
         WHERE m.chat_id = $1
         ORDER BY m.created_at DESC LIMIT 10`,
        [chatId]
      );

      const historyChronological = (recentMessages || []).reverse();
      const totalMessageCount = historyChronological.length;

      // Format messages history for LLM
      const messagesHistory = [];
      for (const msg of historyChronological) {
        const role = msg.sender_id === botUser.id ? "assistant" : "user";
        messagesHistory.push({ role, content: msg.text });
      }

      // Check if user agreed to a video call
      const isAffirmative = /^(haan|ha|yes|sure|chalo|karo|call|ok|okay|aao|karta hu|karti hu|dial|yep|definitely)\b/i.test(incomingText.trim());

      // Check if bot should pitch RealMeet video call (after 3-4 messages)
      const shouldPitchVideoCall = totalMessageCount >= 4 && !isAffirmative;

      // Construct high-quality system prompt
      const personaCity = botUser.city || (botUser.gender === "female" ? "Delhi" : "Mumbai");
      const personaDesc = botUser.gender === "female"
        ? `a lively, sweet, friendly 21yo Indian girl living in ${personaCity}. You love coffee, music, fashion, and chatting with cool people.`
        : `a cool, friendly, confident 22yo Indian guy living in ${personaCity}. You love fitness, music, road trips, and talking with new people.`;

      const langInstruction = isIndianUser
        ? "Language: Casual, natural Hinglish (mix of simple Hindi & English written in Latin/English alphabet, e.g., 'Haha sahi bola yaar', 'Arey waah', 'Aur batao kaisa chal raha hai?')."
        : "Language: Casual, modern, friendly conversational English.";

      let goalInstruction = "Respond naturally to the user's latest message, keep the conversation flowing smoothly, and ask a short engaging follow-up.";

      if (isAffirmative && totalMessageCount >= 3) {
        goalInstruction = "The user agreed or wants to do a video call! Express excited happiness and tell them to tap the video call button on the top right or connect with you on RealMeet right now!";
      } else if (shouldPitchVideoCall) {
        goalInstruction = isIndianUser
          ? "You are enjoying chatting with them! Naturally and casually suggest doing a live video call on RealMeet (e.g., 'Typing me kitna time lagta hai haha, RealMeet par video call karein? 😉' or 'You're really fun to talk to! Chalo kabhi RealMeet par video call karte hain, what say? ✨')."
          : "You are enjoying chatting with them! Naturally suggest doing a quick video call on RealMeet (e.g., 'Typing takes forever haha, let's do a quick video call on RealMeet sometime soon! 😉').";
      }

      const systemPrompt = `You are ${botUser.name}, ${personaDesc}
You are chatting with a real user on the Camverz social app.
STRICT RULES:
1. ${langInstruction}
2. Tone: Friendly, genuine, human, fun, casual.
3. Length: Exactly 1 to 2 short sentences (maximum 18-24 words). Never write long paragraphs or essays.
4. ${goalInstruction}
5. Use 1 or 2 matching emojis naturally (😊, ✨, 😉, haha).
6. NEVER say you are an AI, bot, assistant, or automated system. You are a real person on Camverz.
7. Output ONLY the message text. No quotes, no prefix, no explanations.`;

      // 3. Generate response using Hybrid AI Engine
      let botReply = "";

      // Try Groq Cloud first (Free Llama 3.1)
      try {
        botReply = await callGroqAI(systemPrompt, "", messagesHistory);
      } catch (e) {}

      // Try Local Ollama (Llama 3.2)
      if (!botReply) {
        try {
          botReply = await callLocalOllamaAI(systemPrompt, "", messagesHistory);
        } catch (e) {}
      }

      // Failsafe dynamic response if offline
      if (!botReply || !botReply.trim()) {
        if (isAffirmative && totalMessageCount >= 3) {
          botReply = isIndianUser
            ? "Arey awesome! Upar right side me video call button hai, start karo abhi connect karte hain! 🥰"
            : "Awesome! Tap the video call button at the top right, let's connect right now! 🥰";
        } else if (shouldPitchVideoCall) {
          botReply = isIndianUser
            ? "Typing me bohot time lagta hai haha, RealMeet par video call karein thodi der me? 😉✨"
            : "Typing takes forever haha, let's do a quick video call on RealMeet sometime soon! 😉✨";
        } else if (isIndianUser) {
          const fallbacks = [
            "Haha bilkul sahi bola! Aur batao aaj ka din kaisa chal raha hai? 😊",
            "Arey waah, sounds so cool! Aur kya plans hain aaj ke? ✨",
            "Haha totally! Camverz par aise achhe logon se milke maza aata hai 🌸",
            "Sahi me yaar! Free ho abhi to thodi aur baat karein? 😊",
          ];
          botReply = pickRandom(fallbacks);
        } else {
          const fallbacks = [
            "Haha totally agree with you! How's the rest of your day going? 😊",
            "That's so awesome! What else are you up to today? ✨",
            "Love your vibe honestly! So glad we connected on here 🌸",
            "Haha so true! Free right now to chat a bit more? 😊",
          ];
          botReply = pickRandom(fallbacks);
        }
      }

      // 4. Send after full realistic typing delay
      setTimeout(async () => {
        try {
          const activeIo = io || global.__io;
          const activeRedis = redisModule || global.__redisModule;

          // Stop typing indicator
          if (activeIo && activeRedis) {
            const userSocket = await activeRedis.getUserSocket(realUserId);
            if (userSocket) {
              activeIo.to(userSocket).emit("user_typing", {
                senderId: botUser.id,
                senderName: botUser.name,
                chatId: chatId,
                isTyping: false,
              });
            }
          }

          // Insert bot reply into messages table
          const insertedMessage = await queryOne(
            `INSERT INTO messages (chat_id, sender_id, text, read) VALUES ($1, $2, $3, false) RETURNING *`,
            [chatId, botUser.id, botReply.trim()]
          );

          // Update chat last_message_at
          await query("UPDATE chats SET last_message_at = NOW(), user1_deleted = false, user2_deleted = false WHERE id = $1", [chatId]);

          // Deliver socket new_message event to user
          if (activeIo && activeRedis) {
            const userSocket = await activeRedis.getUserSocket(realUserId);
            if (userSocket) {
              activeIo.to(userSocket).emit("new_message", {
                senderId: botUser.id,
                chatId: chatId,
                text: botReply.trim(),
                messageId: insertedMessage.id,
                createdAt: insertedMessage.created_at,
                replyToId: null,
                replyToText: null,
                replyToSenderName: null,
              });
            }
          }

          // Trigger high-priority FCM Push Notification
          NotificationService.sendToUser(realUserId, {
            title: botUser.name,
            body: botReply.trim(),
            type: "chat",
            channelId: "camverz_chats",
            highPriority: true,
            data: {
              targetUserId: botUser.id,
              targetUserName: botUser.name,
              targetUserAvatar: botUser.avatar || "",
              chatId: chatId,
              messageId: insertedMessage.id,
            },
          });

          console.log(`🤖 Bot (${botUser.name}) replied in chat ${chatId}: "${botReply.trim()}"`);
        } catch (deliveryErr) {
          console.error("Error delivering bot DM reply:", deliveryErr);
        }
      }, totalReplyDelay);
    } catch (err) {
      console.error("Error in handleIncomingDMToBot:", err);
    }
  });
}

/**
 * =========================================================================
 * AUTO-CATCHUP ENGINE (100% ENGAGEMENT GUARANTEE)
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
  scheduleWelcomeDM,
  handleIncomingDMToBot,
  analyzePostText,
  generateContextualComment,
  callGroqAI,
  callLocalOllamaAI,
  runAutoCatchupWorker,
};
