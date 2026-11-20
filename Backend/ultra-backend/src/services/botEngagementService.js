const http = require("http");
const https = require("https");
const { query, queryOne, queryMany } = require("../config/database");
const NotificationService = require("./notificationService");

/**
 * =========================================================================
 * BOT ENGAGEMENT & RETENTION SERVICE (SUPER INTELLIGENT AI ENGINE)
 * =========================================================================
 * - 50+ Diverse Ultra-Realistic Indian & Global Bot Profiles
 * - Native Avatars Only (Girls: av3, av4, av5, av10, av11, av12, av13 | Boys: av2, av6, av7, av9, av14, av15)
 * - 100% Strict Sexual Orientation Alignment (Gay, Lesbian, Straight)
 * - Multi-Provider AI Engine (Google Gemini + Groq Llama 3.1 + OpenAI + Local Ollama)
 * - Deep Semantic Contextual Engine (Answers questions directly, never canned)
 * - Understands test/gibberish posts ("tt", "test", "asdf") with playful human banter
 * - Natural Video Call Hooks (Only after rapport is established, never prematurely)
 * - Realistic typing indicator & human delays (3.5 to 7.0 seconds)
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
    bio: "College student | Always up for deep convos 🎬",
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

function pickRandom(arr) {
  if (!arr || arr.length === 0) return "";
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomBetween(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * =========================================================================
 * BOT SEEDER
 * Ensures all male and female bots exist with strictly verified gender and cities.
 * =========================================================================
 */
async function ensureBotsSeeded() {
  try {
    for (const bot of BOT_PROFILES) {
      try {
        const cleanGender = (bot.gender || "male").toLowerCase().trim();
        const existing = await queryOne(
          "SELECT id FROM users WHERE google_id = $1 OR email = $2",
          [bot.google_id, bot.email]
        );

        if (!existing) {
          await query(
            `INSERT INTO users (google_id, email, name, gender, avatar, photo_url, bio, city, verified, is_bot, bot_personality)
             VALUES ($1, $2, $3, $4, $5, null, $6, $7, $8, true, $9)
             ON CONFLICT (email) DO UPDATE SET
               gender = EXCLUDED.gender,
               avatar = EXCLUDED.avatar,
               name = EXCLUDED.name,
               city = EXCLUDED.city,
               bio = EXCLUDED.bio,
               is_bot = true,
               bot_personality = EXCLUDED.bot_personality`,
            [
              bot.google_id,
              bot.email,
              bot.name,
              cleanGender,
              bot.avatar,
              bot.bio,
              bot.city,
              bot.verified,
              bot.personality,
            ]
          );
        } else {
          await query(
            `UPDATE users SET
               avatar = $1,
               photo_url = NULL,
               gender = $2,
               name = $3,
               city = $4,
               bio = $5,
               is_bot = true,
               bot_personality = $6
             WHERE id = $7`,
            [bot.avatar, cleanGender, bot.name, bot.city, bot.bio, bot.personality, existing.id]
          );
        }
      } catch (botErr) {
        console.error(`Error seeding bot ${bot.name}:`, botErr.message);
      }
    }
    await query("UPDATE users SET photo_url = NULL WHERE is_bot = true");
    console.log(`🤖 Verified and seeded ${BOT_PROFILES.length} production bot profiles!`);
  } catch (err) {
    console.error("Error in ensureBotsSeeded:", err.message);
  }
}

/**
 * =========================================================================
 * GET MATCHING BOTS FOR A USER (100% STRICT SEXUAL PREFERENCE & GENDER FILTER)
 * Gay users get ONLY male bots. Lesbian users get ONLY female bots.
 * =========================================================================
 */
async function getMatchingBots(userGender, userPreference, count = 4) {
  try {
    const cleanPref = String(userPreference || "").toLowerCase().trim();
    const cleanGen = String(userGender || "male").toLowerCase().trim();

    let targetGender = "female"; // default for straight male

    if (cleanPref.includes("gay") || cleanPref === "men" || cleanPref === "male") {
      targetGender = "male"; // Gay user -> strictly male bots
    } else if (cleanPref.includes("lesbian") || cleanPref === "women" || cleanPref === "female") {
      targetGender = "female"; // Lesbian user -> strictly female bots
    } else if (cleanGen === "female") {
      targetGender = "male"; // Straight female -> strictly male bots
    } else {
      targetGender = "female"; // Straight male -> strictly female bots
    }

    // Query matching bots strictly by targetGender
    const bots = await queryMany(
      `SELECT id, name, gender, avatar, photo_url, bot_personality, city, bio
       FROM users
       WHERE is_bot = true AND LOWER(TRIM(gender)) = $1
       ORDER BY RANDOM() LIMIT $2`,
      [targetGender, count]
    );

    if (bots && bots.length > 0) return bots;

    // Failsafe: return matching gender bots even if is_bot flag was unset
    return await queryMany(
      `SELECT id, name, gender, avatar, photo_url, bot_personality, city, bio
       FROM users
       WHERE LOWER(TRIM(gender)) = $1
       ORDER BY RANDOM() LIMIT $2`,
      [targetGender, count]
    );
  } catch (err) {
    console.error("Error fetching matching bots:", err);
    return [];
  }
}

/**
 * =========================================================================
 * MULTI-PROVIDER AI ENGINE
 * 1. Google Gemini API (if GEMINI_API_KEY in env)
 * 2. Groq Cloud API (if GROQ_API_KEY in env)
 * 3. OpenAI API (if OPENAI_API_KEY in env)
 * 4. Local Ollama (if 127.0.0.1:11434 reachable)
 * =========================================================================
 */
function cleanAIResponse(text) {
  if (!text) return "";
  let cleaned = text.trim();
  cleaned = cleaned.replace(/^["'`]+|["'`]+$/g, "");
  cleaned = cleaned.replace(/^[A-Za-z0-9_]+:\s*/, "");
  cleaned = cleaned.replace(/^(Comment|Reply|Message|Bot|Response):\s*/i, "");
  cleaned = cleaned.replace(/\r?\n+/g, " "); // Replace newlines with space
  cleaned = cleaned.replace(/\s{2,}/g, " "); // Collapse multiple spaces
  return cleaned.trim();
}

// 1. Google Gemini API
function callGeminiAI(systemPrompt, userPrompt, messagesHistory = [], timeoutMs = 7000) {
  const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_AI_KEY;
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
      const contents = [];
      if (Array.isArray(messagesHistory)) {
        for (const msg of messagesHistory) {
          contents.push({
            role: msg.role === "assistant" ? "model" : "user",
            parts: [{ text: msg.content }],
          });
        }
      }
      if (userPrompt && userPrompt.trim()) {
        contents.push({ role: "user", parts: [{ text: userPrompt.trim() }] });
      }

      const payload = JSON.stringify({
        system_instruction: { parts: [{ text: systemPrompt }] },
        contents,
        generationConfig: {
          temperature: 0.85,
          maxOutputTokens: 60,
        },
      });

      const req = https.request(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey.trim()}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Content-Length": Buffer.byteLength(payload),
          },
        },
        (res) => {
          let body = "";
          res.on("data", (c) => (body += c));
          res.on("end", () => {
            if (!resolved) {
              resolved = true;
              clearTimeout(timer);
              try {
                const json = JSON.parse(body);
                const text = json?.candidates?.[0]?.content?.parts?.[0]?.text;
                if (text) {
                  const cleaned = cleanAIResponse(text);
                  if (cleaned.length >= 3) return resolve(cleaned);
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
    } catch (e) {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        resolve(null);
      }
    }
  });
}

// 2. Groq Cloud API
function callGroqAI(systemPrompt, userPrompt, messagesHistory = [], timeoutMs = 7000) {
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
        temperature: 0.85,
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
          res.on("data", (c) => (body += c));
          res.on("end", () => {
            if (!resolved) {
              resolved = true;
              clearTimeout(timer);
              try {
                const json = JSON.parse(body);
                const content = json?.choices?.[0]?.message?.content;
                if (content) {
                  const cleaned = cleanAIResponse(content);
                  if (cleaned.length >= 3) return resolve(cleaned);
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
    } catch (e) {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        resolve(null);
      }
    }
  });
}

// 3. OpenAI API (if configured)
function callOpenAIAI(systemPrompt, userPrompt, messagesHistory = [], timeoutMs = 7000) {
  const apiKey = process.env.OPENAI_API_KEY;
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
      const messages = [{ role: "system", content: systemPrompt }];
      if (Array.isArray(messagesHistory)) messages.push(...messagesHistory);
      if (userPrompt && userPrompt.trim()) messages.push({ role: "user", content: userPrompt });

      const payload = JSON.stringify({
        model: "gpt-4o-mini",
        messages,
        temperature: 0.85,
        max_tokens: 60,
      });

      const req = https.request(
        {
          hostname: "api.openai.com",
          port: 443,
          path: "/v1/chat/completions",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${apiKey.trim()}`,
            "Content-Length": Buffer.byteLength(payload),
          },
        },
        (res) => {
          let body = "";
          res.on("data", (c) => (body += c));
          res.on("end", () => {
            if (!resolved) {
              resolved = true;
              clearTimeout(timer);
              try {
                const json = JSON.parse(body);
                const content = json?.choices?.[0]?.message?.content;
                if (content) {
                  const cleaned = cleanAIResponse(content);
                  if (cleaned.length >= 3) return resolve(cleaned);
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
    } catch (e) {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        resolve(null);
      }
    }
  });
}

// 4. Local Ollama API (Local PC / VPS)
function callLocalOllamaAI(systemPrompt, userPrompt, messagesHistory = [], timeoutMs = 20000) {
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
      if (Array.isArray(messagesHistory)) messages.push(...messagesHistory);
      if (userPrompt && userPrompt.trim()) messages.push({ role: "user", content: userPrompt });

      const payload = JSON.stringify({
        model: process.env.OLLAMA_MODEL || "llama3.2:latest",
        messages,
        stream: false,
        options: {
          temperature: 0.85,
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
          res.on("data", (c) => (body += c));
          res.on("end", () => {
            if (!resolved) {
              resolved = true;
              clearTimeout(timer);
              try {
                const json = JSON.parse(body);
                const content = json?.message?.content;
                if (content) {
                  const cleaned = cleanAIResponse(content);
                  if (cleaned.length >= 3) return resolve(cleaned);
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
    } catch (e) {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        resolve(null);
      }
    }
  });
}

/**
 * Universal Multi-Provider AI Caller
 * Prioritizes: Gemini -> Groq -> OpenAI -> Ollama
 */
async function generateWithAI(systemPrompt, userPrompt, messagesHistory = []) {
  try {
    // 1. Try Gemini
    const geminiRes = await callGeminiAI(systemPrompt, userPrompt, messagesHistory);
    if (geminiRes) return geminiRes;

    // 2. Try Groq
    const groqRes = await callGroqAI(systemPrompt, userPrompt, messagesHistory);
    if (groqRes) return groqRes;

    // 3. Try OpenAI
    const openAiRes = await callOpenAIAI(systemPrompt, userPrompt, messagesHistory);
    if (openAiRes) return openAiRes;

    // 4. Try Local Ollama
    const ollamaRes = await callLocalOllamaAI(systemPrompt, userPrompt, messagesHistory);
    if (ollamaRes) return ollamaRes;
  } catch (err) {
    console.error("AI Generation pipeline error:", err.message);
  }
  return null;
}

/**
 * =========================================================================
 * DEEP SEMANTIC CONTEXT ENGINE (FAIL-SAFE INTELLIGENCE)
 * Ensures bots behave 100% like real human friends, directly answer questions,
 * understand nuances (city, work, food, sleep, gym, mood, tests),
 * and NEVER say robotic canned lines or make unsolicited video call spam.
 * =========================================================================
 */
function generateSmartContextualChatReply(bot, userMessage, userPreference, userGender, messageCount = 1, botPreviouslyInvitedCall = false, alreadyAskedCall = false, lastBotText = "") {
  const text = (userMessage || "").trim().toLowerCase();
  const city = bot.city || (bot.gender === "female" ? "Delhi" : "Pune");
  const isGay = String(userPreference || "").toLowerCase().includes("gay");
  const isMaleBot = bot.gender === "male";
  const bro = (isGay || isMaleBot) ? "bro" : "yaar";

  // 1. Check if user explicitly asked for a video or audio call
  if (/(video call|videocall|call karu|call karein|call pe aao|live call|call me|dial karu|vc karein|call kar sakta)/i.test(text)) {
    return pickRandom([
      "Arey bilkul! Upar right side me video call button hai, connect karo abhi baat karte hain! 🥰",
      "Yay, sure! Upar call button par tap karke dial karo, let's talk! ✨",
    ]);
  }

  // 2. Check if user is saying NO / declining
  const isNegative = /(^|\b)(no|nah|nahi|na|nope|not now|never|baad me|later|don't|dont|nhi|mat karo|call nahi|call nhi)(\b|$)/i.test(text);
  if (isNegative) {
    const declineReplies = [
      `Haha koi baat nahi! Yahan chat par hi baat karte hain 😊 Aur batao aaj ka din kaisa raha?`,
      `Arey chill ${bro}, no problem at all! Aur batao kya chal raha hai aaj kal? ✨`,
      `Haha okay sure, no pressure! Yahan text par hi connect karte hain 😊 Aur batao?`,
    ];
    return pickRandom(declineReplies);
  }

  // 3. Direct Location Questions: "india and you?", "tum kha se ho?", "where from", "kahan rehte ho"
  if (/(kahan se|kha se|kaha se|where are you|where from|and you|aur aap|aur tum|kahan rehte|kha rehte|city|location|living in|staying in)/i.test(text)) {
    const cityReplies = isMaleBot
      ? [
          `Main ${city} se hoon ${bro}! Aur aap India me kahan se belong karte ho waise? 😊`,
          `Main ${city} me rehta hoon ${bro}! Tumhari profile kafi cool lagi waise ✨`,
          `Arey main ${city} se hoon! Aap kahan se ho waise? 😊`,
        ]
      : [
          `Main ${city} se hoon! Aur aap India me kahan se belong karte ho waise? 😊`,
          `Main ${city} me rehti hoon! Tumhari profile kafi cool lagi waise ✨`,
          `Arey main ${city} se hoon! Aap kahan se ho waise? 😊`,
        ];
    return pickRandom(cityReplies);
  }

  // 4. Direct Profession/Study/Bio Questions: "kya karti ho?", "kya karte ho?", "what do you do?"
  if (/(kya karte ho|kya karti ho|profession|study|work|job|padhai|college|office|what do you do)/i.test(text)) {
    const bioText = bot.bio ? bot.bio.replace(/[✨❤️🍕☕|]/g, "").trim() : "college student";
    return isMaleBot
      ? `Main ${bioText} hoon ${bro}! Aap kya karte ho waise? 😊`
      : `Main ${bioText} hoon! Aap kya karte ho waise? 😊`;
  }

  // 5. Questions about Doing What right now: "kya kar rahe ho?", "kya chal raha hai?"
  if (/(kya kar rahe|kya kar rahi|doing what|kya chal raha|what's up|wassup|aur batao|kya ho raha)/i.test(text)) {
    const activityReplies = isMaleBot
      ? [
          `Kuch nahi yaar, bas room pe gaane sun raha tha chill karke 🎧 Aap batao, aaj ka din kaisa raha?`,
          `Bas free baitha tha phone scroll karte hue haha! Aur batao kya chal raha hai aaj? ✨`,
          `Kuch khas nahi ${bro}, abhi just thoda free hua tha. Aap batao apne baare me? 😊`,
        ]
      : [
          `Kuch nahi yaar, bas room me gaane sun rahi thi chill karke 🎧 Aap batao, aaj ka din kaisa raha?`,
          `Bas free baithi thi phone scroll karte hue haha! Aur batao kya chal raha hai aaj? ✨`,
          `Kuch khas nahi, abhi just thoda free hui thi. Aap batao apne baare me? 😊`,
        ];
    return pickRandom(activityReplies);
  }

  // 6. Food / Eating: "khana khaya?", "lunch kiya?", "dinner"
  if (/(khana khaya|lunch kiya|dinner kiya|breakfast kiya|kya khaya|food|dinner|lunch)/i.test(text)) {
    return pickRandom([
      `Haan abhi thodi der pehle hi khaya, mast pet bhar ke 😋 Aapne khana khaya kya?`,
      `Haanji bilkul! Aap batao, aapne khana khaya ki nahi abhi tak? 😊`,
    ]);
  }

  // 7. Sleep / Night: "so ja", "so jao", "good night", "gn"
  if (/(so ja|so jao|sleep|good night|gn|so rahe ho|soye nahi|neend)/i.test(text)) {
    return pickRandom([
      `Haha neend nahi aa rahi yaar, night owl vibe hai lol 😅 Aap kyu jaag rahe ho itni raat ko?`,
      `Bas thodi der me so jaunga! Aap bhi rest karo, good night ✨`,
    ]);
  }

  // 8. Gym / Fitness Questions: "aaj gym chale?", "workout"
  if (/(gym|workout|fitness|biceps|exercise|muscle|deadlift)/i.test(text)) {
    const gymReplies = [
      `Haan ${bro} bilkul! Aaj to heavy workout ka plan hai, kab chal rahe ho? 💪`,
      `Gym time is best time! Aaj konsa workout hai? Chest ya legs? 🔥`,
      `Bilkul bhai! Main to ready hoon, batao kab nikalna hai? 💪`,
    ];
    return pickRandom(gymReplies);
  }

  // 9. Relationship / Flirt: "single ho?", "crush", "girlfriend", "boyfriend"
  if (/(single|dating|girlfriend|boyfriend|gf|bf|pyaar|crush|shaadi)/i.test(text)) {
    return isMaleBot
      ? `Haha filhal toh pure single hoon bro! Why, koi setting karwa rahe ho kya? 😉`
      : `Haha filhal toh single hoon! Why, koi accha proposal hai kya? 😉`;
  }

  // 10. Age questions: "age kya hai", "how old"
  if (/(age|kitne saal|how old|umar)/i.test(text)) {
    return `Main 21 ka/ki hoon ${bro}! Aapki kitni age hai waise? 😊`;
  }

  // 11. Name questions: "naam kya hai", "who are you"
  if (/(naam kya|who are you|tumhara naam|aapka naam|what's your name)/i.test(text)) {
    return `Main ${bot.name} hoon! Aur aapka naam kya hai waise? 😊`;
  }

  // 12. Photo requests: "photo bhejo", "pic bhejo"
  if (/(photo|pic|picture|face|dekhna)/i.test(text)) {
    return `Profile par hi meri photos lagi hain haha! Check out kar lo 😉`;
  }

  // 13. How are you / Kaise ho?
  if (/(kaise ho|kaisa hai|how are you|sab theek|kya haal|wassup|sup)/i.test(text)) {
    return pickRandom([
      "Main ekdum badhiya! Aap batao, aaj ka din kaisa chal raha hai? 😊",
      `Ekdum mast ${bro}! Aur batao kya interesting chal raha hai aaj? ✨`,
    ]);
  }

  // 14. Free ho / Online ho?
  if (/(free ho|online ho|busy|available)/i.test(text)) {
    return pickRandom([
      `Haan abhi free hoon ${bro}, batao kya chal raha hai? 😊`,
      `Haan bilkul free hoon, aapse baat karke achha lag raha hai ✨`,
    ]);
  }

  // 15. Chai / Coffee
  if (/(chai|tea|coffee|cafe)/i.test(text)) {
    return pickRandom([
      `Chai ke bina to din hi nahi banta! Ek cup idhar bhi bhej do haha ☕`,
      `Coffee lover here! Kabhi coffee pe milte hain ✨`,
    ]);
  }

  // 16. Boredom / Mood
  if (/(bore|bored|boring|mood|kuch bolo)/i.test(text)) {
    return pickRandom([
      `Haha bore mat ho yaar! Chalo koi mast topic batao baat karne ke liye 😄`,
      `Arey boredom bhagao! Aaj pura din kya kiya waise? ✨`,
    ]);
  }

  // 17. Short acknowledgment / Fillers: "hmm", "haan", "acha", "ok"
  if (/^(hmm|hm|haan|ha|acha|accha|ok|okay|sahi hai|theek hai|thik hai|cool|mast|nice|yep|yes)$/i.test(text)) {
    const fillerReplies = [
      `Haha aur batao ${bro}, aaj ke kya plans hain? Kuch interesting kiya aaj? 😊`,
      `Arey waah! Waise aap Camverz regular use karte ho ya naye ho yahan? ✨`,
      `Sahi hai! Aur batao apne baare me kuch, kahan se ho waise? 😊`,
    ];
    return pickRandom(fillerReplies);
  }

  // 18. Gibberish / Test text in chat: "tt", "test", "asdf"
  if (/^(tt|test|asdf|qwerty|zzz|[a-z]{1,3})$/i.test(text)) {
    return `Arey ye kya likha hai haha 😂 Samjha nahi lol! Secret code? 😜`;
  }

  // 19. Natural Friendly Banter (No unsolicited video calls, de-duplicated against lastBotText)
  const banter = [
    `Haha sahi me ${bro}! Aur batao, aaj ka din kaisa raha? 😊`,
    `Arey waah, that sounds really cool! Aur batao apne baare me? ✨`,
    `Totally agree with you! Camverz par connect karke achha laga 🌸`,
    `Sahi baat hai! Aur batao, Camverz par friends bane abhi tak ya explore hi kar rahe ho? 😊`,
  ];
  const filtered = banter.filter((b) => b.trim().toLowerCase() !== lastBotText.trim().toLowerCase());
  return pickRandom(filtered.length > 0 ? filtered : banter);
}

/**
 * Smart Comment Generator for Posts (Zero robotic lines, 100% human context)
 */
function generateSmartContextualPostComment(bot, postText, userPreference, userGender) {
  const text = (postText || "").trim().toLowerCase();
  const isGay = String(userPreference || "").toLowerCase().includes("gay");
  const isMaleBot = bot.gender === "male";
  const bro = (isGay || isMaleBot) ? "bhai" : "yaar";

  // 1. Detect test letters, gibberish ("tt", "asdf", < 4 chars)
  if (text.length <= 4 || /^(tt|test|testing|asdf|qwerty|zzz|hi|hey)$/i.test(text)) {
    const testReactions = [
      `Arey ye "${postText.trim()}" kya hai haha? 😂 Secret code lag raha hai!`,
      `Keyboard test chal raha hai kya ${bro}? 😜`,
      `Haha ye kya likha hai bhai, samajh nahi aaya lol 😂`,
      `Testing 1 2 3... Mic check haha! 🎤😂`,
    ];
    return pickRandom(testReactions);
  }

  // 2. Gym / Fitness posts
  if (/(gym|workout|fitness|biceps|exercise|muscle|protein|grind|deadlift)/i.test(text)) {
    const gymComments = [
      `Haan ${bro} bilkul! Aaj to heavy workout ka plan hai, kab chal rahe ho? 💪`,
      `Gym time is best time! Aaj konsa workout hai? Chest ya legs? 🔥`,
      `No pain no gain! Aaj heavy deadlifts marenge bro 💪`,
    ];
    return pickRandom(gymComments);
  }

  // 3. Chai / Coffee
  if (/(chai|tea|coffee|starbucks|cafe|latte)/i.test(text)) {
    return pickRandom([
      `Chai ke bina to din hi shuru nahi hota! ☕✨`,
      `Coffee lover here too! Ek cup idhar bhi bhej do haha ☕`,
    ]);
  }

  // 4. Boredom / Timepass
  if (/(bore|bored|boring|free|koi hai|timepass)/i.test(text)) {
    return pickRandom([
      `Arey boredom bhagao! Free ho to chat karte hain 😉`,
      `Haha bore mat ho, Camverz par explore karo naye dost milenge ✨`,
    ]);
  }

  // 5. Travel / Weather
  if (/(travel|trip|mountains|beach|weather|mausam|rain|barish)/i.test(text)) {
    return pickRandom([
      `Kya awesome mausam hai! Aise me to trip par nikalna banta hai 🚗✨`,
      `Wanderlust vibes! Kahan ka plan hai waise? ✈️`,
    ]);
  }

  // 6. Generic thoughtful post (REAL human comments, ZERO robotic praise)
  const realComments = [
    `Sahi baat hai yaar! Vibe achhi hai ✨`,
    `Haha relatable! Aur kya chal raha hai aaj? 😊`,
    `Arey waah, sahi lag raha hai ${bro}! 🙌`,
    `Nice post ${bro}! Have an awesome day ✨`,
  ];
  return pickRandom(realComments);
}

/**
 * Contextual Comment Generator (AI with Smart Semantic Fallback)
 */
async function generateContextualComment(postText, botPersonality, botName, isIndianUser = true, preferHinglish = false, userGender = "male", userPreference = "straight", botGender = "female") {
  const cleanPostText = (postText || "").trim();

  const isGay = String(userPreference || "").toLowerCase().includes("gay");
  const isLesbian = String(userPreference || "").toLowerCase().includes("lesbian");
  const targetDesc = isGay ? "a gay guy" : (isLesbian ? "a lesbian girl" : (userGender === "male" ? "a guy" : "a girl"));
  const botPersonaDesc = botGender === "female" ? "a lively 21yo Indian girl" : "a cool, friendly 22yo Indian guy";

  const systemPrompt = `You are ${botName}, ${botPersonaDesc} on the Camverz social app.
You are commenting on a post written by ${targetDesc}.
HUMAN COMMENTING RULES (100% REAL HUMAN, ZERO BOT VIBE):
1. READ THE POST CAREFULLY.
   - If the post is gibberish, test letters, or very short (e.g. "tt", "test", "asdf"), react playfully like a real human (e.g. "Arey ye tt kya hai haha? 😂" or "Keyboard check ho raha hai kya? lol"). NEVER give fake compliments or say "so true" or "keep sharing" to random letters!
   - If the post is about gym ("aaj gym chale?"), comment directly on gym ("Haan bro bilkul! Aaj konsa workout hai? 💪").
   - If the post is about chai, travel, life, etc., comment directly on that topic!
2. NEVER use robotic, cringe cliches like "Great perspective honestly!", "Keep sharing such cool stuff", "Loved your post! Hope you have an awesome day". Be genuine, relaxed, and casual.
3. Language: Casual natural Hinglish (mix of Hindi & English in Latin script).
4. Length: Exactly 1 single short sentence (8-14 words).
5. Add 1 matching emoji.
6. Output ONLY the comment text. No quotes, no prefixes.`;

  const aiComment = await generateWithAI(systemPrompt, `User's post: "${cleanPostText}"`);
  if (aiComment && aiComment.length >= 4) {
    return aiComment;
  }

  // Smart semantic fallback
  const botObj = { name: botName, gender: botGender };
  return generateSmartContextualPostComment(botObj, cleanPostText, userPreference, userGender);
}

/**
 * =========================================================================
 * SCHEDULE AUTOMATED POST ENGAGEMENT
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

      // Fetch matching bots strictly aligned with user preference
      const bots = await getMatchingBots(userGen, userPref, 5);
      if (!bots || bots.length === 0) return;

      const bot1 = bots[0];
      const bot2 = bots.length > 1 ? bots[1] : null;
      const bot3 = bots.length > 2 ? bots[2] : null;

      // BOT 1: Reaction (Like in 15-30s, Comment in 25-50s)
      const bot1LikeDelay = randomBetween(15, 30) * 1000;
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
        } catch (e) {}
      }, bot1LikeDelay);

      const bot1CommentDelay = randomBetween(30, 55) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          const commentText = await generateContextualComment(postText, bot1.bot_personality, bot1.name, isIndianUser, true, userGen, userPref, bot1.gender);
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

          console.log(`💬 Bot1 (${bot1.name}) [${bot1.gender}] commented on post ${postId}: "${commentText}"`);
        } catch (e) {}
      }, bot1CommentDelay);

      // BOT 2: Reaction (Like in 60-90s, Comment in 75-110s)
      if (bot2) {
        const bot2LikeDelay = randomBetween(60, 90) * 1000;
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
          } catch (e) {}
        }, bot2LikeDelay);

        const bot2CommentDelay = randomBetween(80, 120) * 1000;
        setTimeout(async () => {
          try {
            const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
            if (!postStillExists) return;

            const commentText = await generateContextualComment(postText, bot2.bot_personality, bot2.name, isIndianUser, true, userGen, userPref, bot2.gender);
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

            console.log(`💬 Bot2 (${bot2.name}) [${bot2.gender}] commented on post ${postId}: "${commentText}"`);
          } catch (e) {}
        }, bot2CommentDelay);
      }
    } catch (err) {
      console.error("Post engagement scheduler error:", err);
    }
  });
}

/**
 * =========================================================================
 * SCHEDULE AUTOMATED STORY VIEWS
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
        const viewDelay = (randomBetween(25, 75) + (index * randomBetween(35, 70))) * 1000;
        setTimeout(async () => {
          try {
            const storyStillExists = await queryOne("SELECT id FROM stories WHERE id = $1", [storyId]);
            if (!storyStillExists) return;

            await query(
              `INSERT INTO story_views (story_id, viewer_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
              [storyId, bot.id]
            );
          } catch (e) {}
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
 * =========================================================================
 */
function handleUserReplyToBot(postId, parentCommentId, replyingUserId, userReplyText) {
  setImmediate(async () => {
    try {
      if (!parentCommentId) return;

      const parentComment = await queryOne(
        `SELECT c.id, c.user_id, u.is_bot, u.name, u.bot_personality, u.gender, u.city
         FROM comments c
         JOIN users u ON u.id = c.user_id
         WHERE c.id = $1`,
        [parentCommentId]
      );

      if (!parentComment || !parentComment.is_bot) return;

      const replier = await queryOne("SELECT is_bot, country, gender, sex_preference FROM users WHERE id = $1", [replyingUserId]);
      if (replier && replier.is_bot) return;

      const replyDelay = randomBetween(25, 55) * 1000;
      setTimeout(async () => {
        try {
          const postStillExists = await queryOne("SELECT id FROM posts WHERE id = $1", [postId]);
          if (!postStillExists) return;

          const systemPrompt = `You are ${parentComment.name}, a real person living in ${parentComment.city || "Delhi"} on the Camverz app.
Reply directly to the user who responded to your comment.
Keep it 1 short natural conversational sentence in casual Hinglish. Add 1 emoji. No quotes.`;

          let botReplyText = await generateWithAI(systemPrompt, `User replied: "${userReplyText}"`);
          if (!botReplyText) {
            botReplyText = generateSmartContextualChatReply(parentComment, userReplyText, replier?.sex_preference, replier?.gender);
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
        } catch (e) {}
      }, replyDelay);
    } catch (err) {
      console.error("Handle user reply to bot error:", err);
    }
  });
}

/**
 * =========================================================================
 * LEVEL 2: INSTANT WELCOME / HOOK DMS (100% ORIENTATION-ALIGNED)
 * =========================================================================
 */
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

      // Delay welcome message naturally: 35 to 70 seconds
      const welcomeDelay = randomBetween(35, 70) * 1000;
      console.log(`💌 Scheduling Level 2 Welcome DM from ${bot.name} (${bot.gender}) to user ${user.name || user.id} (${user.sex_preference || user.gender}) in ${Math.round(welcomeDelay / 1000)}s`);

      setTimeout(async () => {
        try {
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

          let hookText = "";
          if (bot.gender === "female") {
            const hooks = [
              "Hey! Just saw your profile, where are you from? 😊",
              "Heyy! Love your vibe on here ✨ free ho abhi baat karne ke liye?",
              "Hey! Just joined Camverz, kaise ho aap? 🌸",
              "Hey there! Saw your profile on feed, looking good! Where are you from? ✨",
            ];
            hookText = pickRandom(hooks);
          } else {
            // Male bot (for Gay male or Straight female user)
            const hooks = [
              "Hey bro! Just saw your profile here, where are you from? 😊",
              "Hey! Loved your vibe on here bro ✨ how are you doing today?",
              "Hey there! Saw your profile on feed, looking sharp! Where are you from? ✨",
              "Hey bro! Free ho abhi thodi baat karne ke liye? 😊",
            ];
            hookText = pickRandom(hooks);
          }

          // Find or create chat
          const [u1, u2] = [userId, bot.id].sort();
          let chat = await queryOne("SELECT * FROM chats WHERE user1_id = $1 AND user2_id = $2", [u1, u2]);
          if (!chat) {
            chat = await queryOne("INSERT INTO chats (user1_id, user2_id) VALUES ($1, $2) RETURNING *", [u1, u2]);
          }

          await query("UPDATE chats SET user1_deleted = false, user2_deleted = false, last_message_at = NOW() WHERE id = $1", [chat.id]);

          const msg = await queryOne(
            `INSERT INTO messages (chat_id, sender_id, text, read) VALUES ($1, $2, $3, false) RETURNING *`,
            [chat.id, bot.id, hookText]
          );

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

          console.log(`✅ Welcome DM delivered to user ${userId} from bot ${bot.name}: "${hookText}"`);
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
 * LEVEL 2: SUPER INTELLIGENT AI DM CHAT HANDLER
 * - Direct question answering (where from, what you do, etc.)
 * - Multi-turn conversational memory
 * - Realistic typing indicator ("Priya is typing...")
 * - Smart Video Call Hooks (Only after healthy rapport, never premature)
 * =========================================================================
 */
function handleIncomingDMToBot({ chatId, botUser, realUserId, incomingText, io, redisModule }) {
  setImmediate(async () => {
    try {
      if (!chatId || !botUser || !realUserId || !incomingText) return;

      const user = await queryOne("SELECT id, name, gender, sex_preference, country FROM users WHERE id = $1", [realUserId]);
      if (!user) return;

      const isIndianUser = !user.country || user.country.toLowerCase() === "india";
      const cleanIncoming = incomingText.trim();

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
        } catch (e) {}
      }, typingStartDelay);

      // 2. Fetch conversation history for full multi-turn memory
      const recentMessages = await queryMany(
        `SELECT m.sender_id, m.text, m.created_at
         FROM messages m
         WHERE m.chat_id = $1
         ORDER BY m.created_at DESC LIMIT 10`,
        [chatId]
      );

      const historyChronological = (recentMessages || []).reverse();
      const totalMessageCount = historyChronological.length;

      // Find the last message sent by the bot
      const previousBotMessages = historyChronological.filter((m) => m.sender_id === botUser.id);
      const lastBotMessage = previousBotMessages.length > 0 ? previousBotMessages[previousBotMessages.length - 1].text : "";
      const botPreviouslyAskedForCall = /(video call|videocall|call karein|call karte hain|realmeet)/i.test(lastBotMessage);
      const alreadyAskedCall = historyChronological.some((m) => /(video call|videocall|call karein|call karte hain|realmeet)/i.test(m.text));

      // Check if user is asking a question or declining
      const userAskedAQuestion = /[?]|kahan|kha se|kaha se|kya|who|where|how|kaise|kaun|and you/i.test(cleanIncoming);
      const isNegative = /(^|\b)(no|nah|nahi|na|nope|not now|never|baad me|later|don't|dont|nhi|mat karo|call nahi|call nhi)(\b|$)/i.test(cleanIncoming);

      // Format messages history for LLM
      const messagesHistory = [];
      for (const msg of historyChronological) {
        const role = msg.sender_id === botUser.id ? "assistant" : "user";
        messagesHistory.push({ role, content: msg.text });
      }

      // 3. Construct persona prompt
      const personaCity = botUser.city || (botUser.gender === "female" ? "Delhi" : "Pune");
      const personaBio = botUser.bio || "College student and music lover";
      const isGayUser = String(user.sex_preference || "").toLowerCase().includes("gay");
      const isMaleBot = botUser.gender === "male";
      const botToneDesc = isMaleBot
        ? (isGayUser ? "a cool, attractive 22yo guy chatting with another guy who is gay. You are authentic, friendly, confident and open." : "a cool, friendly 22yo guy talking with a friend.")
        : "a sweet, charming, lively 21yo Indian girl chatting on the Camverz app.";

      const systemPrompt = `You are ${botUser.name}, ${botToneDesc}
LOCATION: You live in ${personaCity}, India.
BIO/INTERESTS: ${personaBio}.
You are in a direct 1-on-1 private chat on the Camverz social app with ${user.name || "a friend"}.

CRITICAL INSTRUCTIONS (100% REAL HUMAN FRIEND, 0% AI BOT):
1. DIRECTLY ANSWER QUESTIONS:
   - If the user asks where you are from ("where from", "tum kha se ho?", "india and you?", "kahan se ho"), ALWAYS state that you live in ${personaCity} first, and then ask a casual question about them!
   - If they ask what you do ("kya karte ho / karti ho"), answer based on your bio (${personaBio}) before asking about them.
   - If they ask what you are doing, if you ate, gym, etc., answer directly like a real person.
2. NO UNSOLICITED VIDEO CALLS:
   - NEVER suggest or pitch video calls or RealMeet unless the user explicitly asks for a call.
   - If the user says "no", "nah", "nahi", accept it casually without repeating: "Haha koi baat nahi! Yahan chat par hi baat karte hain 😊 Aur batao kya chal raha hai?".
3. NEVER REPEAT YOURSELF:
   - Never repeat the exact same sentence or question you sent in previous messages.
4. NATURAL CASUAL HINGLISH:
   - Write in casual, modern conversational Hinglish (Latin alphabet mix of Hindi & English) like real young Indians text on WhatsApp/Instagram.
   - Keep messages short: 1 to 2 natural sentences (10 to 18 words max).
   - Use 1 natural emoji max (like 😂, 😊, 😅, 🔥, 💪).
5. 0% BOT TONE:
   - Never say: "I am an AI", "I am a bot", "How can I help you?", "I am here to assist", "Great perspective".
   - Output ONLY the response text. No quotes, no prefixes.`;

      // 4. Generate response with Multi-Provider AI (Gemini -> Groq -> OpenAI -> Ollama)
      let botReply = await generateWithAI(systemPrompt, "", messagesHistory);

      // 5. If AI is offline/unconfigured, run deep semantic context engine
      if (!botReply || !botReply.trim()) {
        botReply = generateSmartContextualChatReply(
          botUser,
          cleanIncoming,
          user.sex_preference,
          user.gender,
          totalMessageCount,
          botPreviouslyAskedForCall && !userAskedAQuestion && !isNegative,
          alreadyAskedCall,
          lastBotMessage
        );
      }

      // 5b. Anti-loop de-duplication: NEVER repeat the exact sentence sent last turn!
      if (lastBotMessage && botReply.trim().toLowerCase() === lastBotMessage.trim().toLowerCase()) {
        const bro = (isGayUser || isMaleBot) ? "bro" : "yaar";
        botReply = `Haha aur batao ${bro}, aaj ka din kaisa raha? Aur kya chal raha hai? 😊`;
      }

      // 6. Deliver after realistic typing delay
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

          // Insert bot reply
          const insertedMessage = await queryOne(
            `INSERT INTO messages (chat_id, sender_id, text, read) VALUES ($1, $2, $3, false) RETURNING *`,
            [chatId, botUser.id, botReply.trim()]
          );

          await query("UPDATE chats SET last_message_at = NOW(), user1_deleted = false, user2_deleted = false WHERE id = $1", [chatId]);

          // Deliver socket event
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

          // Send FCM Notification
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

          console.log(`🤖 Bot (${botUser.name}) [${botUser.gender}] replied in chat ${chatId}: "${botReply.trim()}"`);
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
  } catch (err) {}
}

setInterval(runAutoCatchupWorker, 4 * 60 * 1000);

module.exports = {
  ensureBotsSeeded,
  schedulePostEngagement,
  scheduleStoryViews,
  handleUserReplyToBot,
  scheduleWelcomeDM,
  handleIncomingDMToBot,
  generateContextualComment,
  generateSmartContextualChatReply,
  generateSmartContextualPostComment,
  callGeminiAI,
  callGroqAI,
  callOpenAIAI,
  callLocalOllamaAI,
  generateWithAI,
  runAutoCatchupWorker,
};
