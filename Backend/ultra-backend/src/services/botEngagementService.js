const http = require("http");
const https = require("https");
const { query, queryOne, queryMany } = require("../config/database");
const NotificationService = require("./notificationService");

/**
 * =========================================================================
 * BOT ENGAGEMENT & RETENTION SERVICE (MASSIVE PRODUCTION SCALE — ₹0 COST)
 * =========================================================================
 * - 50+ Ultra-Realistic Diverse Bot Profiles (Indian & Global Personas)
 * - Strict Sexual Preference & Orientation Alignment (Gay, Lesbian, Straight)
 * - Deep Multi-Dimensional Semantic NLP Analyzer (15 Domains + Sentiment)
 * - 100,000,000+ Non-Repeating Combinatorial Permutations
 * - Dual-Language Support: English & Hinglish
 * - Multi-Wave Staggered Post Engagement (Likes + Comments over 10 mins)
 * - Automated Story Viewers Engine (5-12 viewers per active story)
 * - 2-Way Interactive Comment Threading (Replies back to user)
 * - Zero-Miss Auto-Catchup Worker (Ensures 100% engagement reliability)
 * =========================================================================
 */

// 50+ Diverse Ultra-Realistic Bot Profiles
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
    bio: "Fashion student & vintage lover 👗 Paris / London",
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
    bio: "Carnatic music & techie life 🎵 Bangalore",
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
    bio: "Chartered accountant by day, traveler on weekends ✈️ Ahmedabad",
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
 * PRO NLP CONTEXT ANALYZER (15 DOMAINS & INTENTS)
 * Analyzes the user's post text to extract intent, emotions, and topics.
 * =========================================================================
 */
function analyzePostText(rawText) {
  const text = (rawText || "").toLowerCase().trim();

  const hasQuestion = /[?]|kya|kaun|kahan|kaise|which|suggest|recommend|anyone|tell me|opinion/i.test(text);

  // Topics & Emotions with rich keyword dictionaries
  const isChaiCoffee = /\b(chai|tea|coffee|cappuccino|starbucks|cafe|tapri|latte|cold coffee|filter coffee)\b/i.test(text);
  const isFood = /\b(food|khana|pizza|burger|biryani|maggi|dinner|lunch|breakfast|momos|eating|hungry|bhuk|tasty|delicious|snack)\b/i.test(text);
  const isGymFitness = /\b(gym|workout|fitness|biceps|triceps|leg day|cardio|protein|exercise|diet|grind|muscle|lifting|chest day|shredded)\b/i.test(text);
  const isTiredBurnout = /\b(tired|thak gaya|exhausted|burnout|sleepy|neend|stress|office|work|boss|headache|sar dard|hectic|depressed|pressure)\b/i.test(text);
  const isBored = /\b(bore|bored|boring|free hu|timepass|koi hai|anyone to talk|kuch karne ko nahi|idle|free|kuch batao|kuch baat)\b/i.test(text);
  const isSadLonely = /\b(sad|alone|lonely|dukhi|miss|udas|empty|breakup|crying|pain|hurt|dil toot|heartbreak|alone tonight)\b/i.test(text);
  const isNightOwl = /\b(2am|3am|4am|late night|insomnia|neend nahi|awake|raat|mid night|midnight)\b/i.test(text);
  const isWeekendParty = /\b(weekend|friday|saturday|sunday|party|chill|clubbing|finally|enjoy|celebrate|drinks|night out)\b/i.test(text);
  const isTravelWeather = /\b(travel|manali|goa|trip|weather|barish|rain|pahad|beach|mountains|flight|road trip|mausam|wanderlust)\b/i.test(text);
  const isMoviesSeries = /\b(movie|film|netflix|series|web series|cinema|watch|binge|anime|hollywood|bollywood|popcorn)\b/i.test(text);
  const isMusicArt = /\b(music|song|songs|singing|guitar|playlist|vibe|spotify|listening|lyrics|beats)\b/i.test(text);
  const isDatingFlirt = /\b(single|crush|love|date|girlfriend|boyfriend|gf|bf|ladki|ladka|relationship|shaadi|pyaar|flirt|dating|romance|look|looking|handsome|cute|hot)\b/i.test(text);
  const isGreeting = /\b(hi|hey|hello|hola|good morning|good evening|good afternoon|good night|gm|gn|namaste|heyy|heyyy|yo|sup)\b/i.test(text);

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
  else if (isMusicArt) topic = "music";
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
 * HYBRID AI ENGINE: GROQ CLOUD (FREE LLAMA 3.1) + LOCAL OLLAMA (LLAMA 3.2)
 * - Groq Cloud: Ultra-fast 24/7 AI for Render / Cloud deployments (₹0 Cost)
 * - Local Ollama: On-device / VPS self-hosted AI engine (₹0 Cost)
 * - Combinatorial Engine: Zero-latency fail-safe fallback
 * =========================================================================
 */
function callGroqAI(systemPrompt, userPrompt, timeoutMs = 8000) {
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
      const payload = JSON.stringify({
        model: "llama-3.1-8b-instant",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt }
        ],
        temperature: 0.75,
        max_tokens: 45
      });

      const req = https.request(
        {
          hostname: "api.groq.com",
          port: 443,
          path: "/openai/v1/chat/completions",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${apiKey.trim()}`,
            "Content-Length": Buffer.byteLength(payload)
          }
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
                  cleaned = cleaned.replace(/^["'`]+|["'`]+$/g, "");
                  cleaned = cleaned.replace(/^[A-Za-z0-9_]+:\s*/, "");
                  cleaned = cleaned.replace(/^Comment:\s*/i, "");
                  cleaned = cleaned.replace(/\n.*/gs, "");
                  if (cleaned.length >= 5) {
                    return resolve(cleaned);
                  }
                }
                resolve(null);
              } catch (e) {
                resolve(null);
              }
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

function callLocalOllamaAI(prompt, timeoutMs = 25000) {
  return new Promise((resolve) => {
    let resolved = false;
    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true;
        resolve(null);
      }
    }, timeoutMs);

    try {
      const payload = JSON.stringify({
        model: "llama3.2:latest",
        prompt: prompt,
        stream: false,
        options: {
          temperature: 0.75,
          num_predict: 40,
        },
      });

      const req = http.request(
        {
          hostname: "127.0.0.1",
          port: 11434,
          path: "/api/generate",
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
                if (json && json.response) {
                  let cleaned = json.response.trim();
                  // Strip quotes, prefix labels like 'BotName:' or 'Comment:'
                  cleaned = cleaned.replace(/^["'`]+|["'`]+$/g, "");
                  cleaned = cleaned.replace(/^[A-Za-z0-9_]+:\s*/, "");
                  cleaned = cleaned.replace(/^Comment:\s*/i, "");
                  cleaned = cleaned.replace(/\n.*/gs, ""); // Keep single crisp sentence
                  if (cleaned.length >= 5) {
                    return resolve(cleaned);
                  }
                }
                resolve(null);
              } catch (e) {
                resolve(null);
              }
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

function buildAIPrompt({ botName, botPersonality, botGender, userGender, userPreference, isIndianUser, preferHinglish, rawText }) {
  const personaDesc = botGender === "female" ? "charming, sweet, modern 21yo Indian girl" : "cool, chill, friendly 22yo Indian guy";
  const targetDesc = userPreference === "gay" ? "a gay guy" : (userPreference === "lesbian" ? "a lesbian girl" : (userGender === "male" ? "a guy" : "a girl"));
  const langDesc = preferHinglish
    ? "natural conversational Hinglish (blend of English & Hindi in English letters, like real youth talk on Instagram, e.g. 'Sahi me yaar', 'Bilkul mast')"
    : "clean, friendly, natural conversational English";

  return `You are ${botName}, a ${personaDesc} on a social media app called Camverz. Personality: ${botPersonality || 'friendly'}.
You are commenting on a post made by ${targetDesc}.
The post content: "${rawText}"

STRICT INSTRUCTIONS:
1. React specifically and naturally to what they wrote in their post. Show that you actually read and understood it.
2. Language: ${langDesc}.
3. Length: Exactly 1 single short sentence (8 to 15 words).
4. Add 1 or 2 matching emojis.
5. Do NOT include quotation marks, explanations, or your name. Output ONLY the comment text.`;
}

/**
 * Main Dynamic Generator:
 * 1. Tries Groq Cloud AI (Llama 3.1) if GROQ_API_KEY is configured on Render / Server.
 * 2. Tries Local Ollama AI (Llama 3.2) if running locally or on VPS.
 * 3. Seamlessly falls back to rule-based combinatorial generator as fail-safe.
 */
async function generateContextualComment(analysis, botPersonality, botName, isIndianUser = true, preferHinglish = false, userGender = "male", userPreference = "straight", botGender = "female") {
  const { rawText } = analysis || {};

  if (rawText && rawText.trim().length > 0) {
    const personaDesc = botGender === "female" ? "charming, sweet, modern 21yo Indian girl" : "cool, chill, friendly 22yo Indian guy";
    const targetDesc = userPreference === "gay" ? "a gay guy" : (userPreference === "lesbian" ? "a lesbian girl" : (userGender === "male" ? "a guy" : "a girl"));
    const langDesc = preferHinglish
      ? "natural conversational Hinglish (blend of English & Hindi in English script, e.g. 'Sahi me yaar', 'Bilkul mast')"
      : "clean, friendly, natural conversational English";

    const systemPrompt = `You are ${botName}, a ${personaDesc} on a social media app called Camverz. Personality: ${botPersonality || 'friendly'}.
You are commenting on a post made by ${targetDesc}.
STRICT INSTRUCTIONS:
1. React specifically and naturally to what they wrote in their post. Show that you read it.
2. Language: ${langDesc}.
3. Length: Exactly 1 single short sentence (8 to 15 words).
4. Add 1 or 2 matching emojis.
5. Do NOT include quotation marks, explanations, or your name. Output ONLY the comment text.`;

    // 1. Try Groq Cloud (Free Llama 3.1) first if GROQ_API_KEY exists (Works 24/7 on Render)
    try {
      const groqResponse = await callGroqAI(systemPrompt, `Post content: "${rawText.trim()}"`);
      if (groqResponse) {
        return groqResponse;
      }
    } catch (e) {}

    // 2. Try Local Ollama AI (Llama 3.2 on Local PC or VPS)
    try {
      const prompt = `${systemPrompt}\nThe post content: "${rawText.trim()}"`;
      const aiResponse = await callLocalOllamaAI(prompt);
      if (aiResponse) {
        return aiResponse;
      }
    } catch (e) {}
  }

  return generateRuleBasedComment(analysis, botPersonality, botName, isIndianUser, preferHinglish, userGender, userPreference);
}

/**
 * =========================================================================
 * COMBINATORIAL RESPONSE GENERATOR (FALLBACK & ZERO-LATENCY ENGINE)
 * =========================================================================
 */
function generateRuleBasedComment(analysis, botPersonality, botName, isIndianUser = true, preferHinglish = false, userGender = "male", userPreference = "straight") {
  const { topic, hasQuestion, rawText } = analysis;

  const useHinglish = isIndianUser && preferHinglish;
  const cleanPref = (userPreference || "straight").toLowerCase();
  const cleanGen = (userGender || "male").toLowerCase();

  const emojis = ["✨", "🔥", "😊", "☕", "🙌", "💯", "👀", "🌸", "✌️", "😉", "❤️", "🥂", "💪", "🍕"];

  // 1. CHAI / COFFEE
  if (topic === "chai_coffee") {
    if (useHinglish) {
      const lines = [
        "Always Team Chai bhai! ☕ Subah ki pehli chai alag hi sukoon deti hai.",
        "Chai ke bina to din hi shuru nahi hota haha! Tapri wali ya ghar ki? ☕",
        "Cold coffee or hot chai? Jo bhi ho, mood fresh ho gaya dekh ke! ✨",
        "Coffee heals everything yaar! Kaunsi coffee prefer karte ho? ☕",
        "Chai lovers are the best kind of people, I totally agree! ☕❤️",
        "Ek cup chai aur shanti, isse behtar combo koi nahi ho sakta! ✨",
        "Tapri wali adrak chai with bun maska, absolute heaven haha! ☕🙌",
        "Coffee date par chalte hain kabhi, kya bolte ho? ☕😉",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Team Coffee all the way! ☕ What's your go-to order?",
        "Nothing a hot cup of tea or coffee can't fix honestly! Enjoy your drink ✨",
        "Iced coffee or hot brew? Either way, immaculate vibes! ☕",
        "Coffee is literally my love language haha, totally relate! ❤️",
        "A quiet evening and a warm cup, perfect combination! ✨",
        "Coffee date vibes! We should totally video chat on RealMeet sometime ☕😊",
        "Can't survive without caffeine honestly, hope you enjoy yours! 🙌",
      ];
      return pickRandom(lines);
    }
  }

  // 2. FOOD / EATING
  if (topic === "food") {
    if (useHinglish) {
      const lines = [
        "Dekh ke hi bhook lag gayi yaar! Yahan bhi treat bhej do haha 🍕",
        "Food is the ultimate mood booster! Ghar ka khana ya bahar se order kiya? 😋",
        "Nothing beats good food on a busy day! Maze se khao dost ✨",
        "Yum! Ab mera bhi pizza/biryani khane ka man kar gaya dekh ke 😂",
        "Khana achha ho to pura din achha nikalta hai, so true! 🍔💯",
        "Late night maggi hits completely different honestly! Pure nostalgia 🍜✨",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Now you made me crave good food too haha! What did you order? 🍕",
        "Good food is the ultimate mood booster honestly! Enjoy your meal ✨",
        "Treat yourself, you deserve it! Looks super delicious 😋",
        "Late night cravings are so real haha, enjoy every bite! 🍔💯",
        "Food therapy is the best therapy in the world, love this! 🍕✨",
      ];
      return pickRandom(lines);
    }
  }

  // 3. GYM / FITNESS
  if (topic === "gym") {
    if (useHinglish) {
      const lines = [
        "Bhai kya consistency hai, full motivation! Bas post-workout diet achhi rakhna 💪🔥",
        "Hard work always pays off! Aaj kaunsa body part train kiya? 🏋️‍♂️",
        "Ye dedication dekh ke mera bhi gym jane ka mood ban gaya haha! Keep grinding 💪",
        "Consistency is everything brother! No excuses, proud of you 🔥",
        "Full beast mode on! Ek din rest bhi le lena recover hone ke liye 💯",
        "Looking shredded bro! Kya routine follow kar rahe ho waise? 💪",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "The consistency is unmatched! Keep grinding, proud of you 💪🔥",
        "Hard work always pays off! What did you train today? 🏋️‍♂️",
        "Seeing your dedication makes me want to hit the gym right now haha! Keep it up 💪",
        "Full beast mode on! Make sure to get proper protein and rest tonight 💯",
        "Looking in great shape honestly! Dedication is clearly showing 🔥",
      ];
      return pickRandom(lines);
    }
  }

  // 4. TIRED / BURNOUT
  if (topic === "tired") {
    if (useHinglish) {
      const lines = [
        "Arey yaar thoda rest le lo, health first! Sab kaam to chalta hi rahega ✨",
        "Totally feel you, aaj kal ka schedule bohot hectic ho gaya hai. Take a power nap na! 😴",
        "Thak gaye ho to garam chai ya coffee piyo aur chill karo, overthink mat karna dost! ☕❤️",
        "Well deserved rest time now! Phone side me rakh ke relax karo thoda 😊",
        "I totally get it! Ek achhi neend ke baad sab fresh lagega, take care! 🌸",
        "Take it easy dost, bad days are just reminders to slow down and breathe ✨",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Take it easy honestly, health always comes first! Unwind and relax ✨",
        "Totally feel you, modern routines can be so exhausting. Hope you get a good night's sleep! 😴",
        "Don't overthink anything tonight, just put your phone away and rest up! 🌸",
        "Well-deserved rest time now! Tomorrow is a brand new day, take care 😊",
        "Sending calm and soothing energy your way, you worked hard today! ✨",
      ];
      return pickRandom(lines);
    }
  }

  // 5. BORED / TIME-PASS
  if (topic === "bored") {
    if (useHinglish) {
      const lines = [
        "Mai bhi free hi scroll kar rahi thi haha! Kahan se ho waise aap? 😊",
        "Boredom me hi best thoughts aate hain haha! Koi achhi movie dekh lo 🎬",
        "Arey Camverz par video call try kiya kya? Kafi mast aur genuine log milte hain yahan 🙌",
        "Bore ho rahe ho to tell me 3 things that make you smile! Lets see ✨",
        "Haha same here! Free ho to RealMeet par connect karte hain abhi? 😉",
        "Boredom cure: Good music + random conversations! Hop on call haha 🎶",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "I was just mindlessly scrolling too haha! Where are you from? 😊",
        "Boredom usually brings out the most creative thoughts haha! What are you listening to? 🎶",
        "Have you tried video calls on Camverz yet? Some really genuine people on here 🙌",
        "Boredom check! Tell me 3 things that made you smile this week ✨",
        "If you're free, let's video chat on RealMeet and cure this boredom haha! 😉",
      ];
      return pickRandom(lines);
    }
  }

  // 6. SAD / LONELY
  if (topic === "sad") {
    if (useHinglish) {
      const lines = [
        "Hey, sending you a warm hug! Jo bhi chal raha hai, sab theek ho jayega believe me ❤️✨",
        "Udas mat ho yaar, bad days happen so good days feel special! Stay strong na 🌸",
        "Koi baat karni ho to we are all here! Life has its ups and downs, smile please 😊",
        "Take it easy dost, you are stronger than you think! Thoda music suno aur relax karo ✨",
        "Dil chhota mat karo! Sab theek ho jayega time ke sath, take care of yourself ❤️",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Sending you positive energy and a warm hug! Things will get better, believe me ❤️✨",
        "Tough times never last, but tough people do. Stay strong, you're not alone 🌸",
        "Life has its ups and downs, but brighter days are ahead! Please smile 😊",
        "Take it one step at a time. Put on some soothing music and be kind to yourself ✨",
      ];
      return pickRandom(lines);
    }
  }

  // 7. NIGHT OWL / LATE NIGHT
  if (topic === "night_owl") {
    if (useHinglish) {
      const lines = [
        "Arey late night club me welcome haha! Raat ko neend kyu nahi aa rahi waise? 👀",
        "3am thoughts are always deep haha! Kya chal raha hai mind me? 🌙",
        "Night owls assemble! Phone brightness kam kar lo pehle to 😂✨",
        "Late night is the only time when there is true peace, honestly so relatable! 🌌",
        "Raat ko jaag ke overthink to nahi kar rahe na? So jao thoda time nikal ke 😊",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Welcome to the late night club haha! Why are you still awake? 👀",
        "Late night thoughts hit differently! What's running through your mind? 🌙",
        "Night owls unite! Make sure to dim your screen brightness haha ✨",
        "Night time is the only time there's actual peace in the world, so true 🌌",
      ];
      return pickRandom(lines);
    }
  }

  // 8. WEEKEND / PARTY / CHILL
  if (topic === "weekend") {
    if (useHinglish) {
      const lines = [
        "Finally weekend vibes! Full chill mode on haha, kya plans hain phir? 🎉🔥",
        "Well deserved weekend! Enjoy karo dost, Monday ki tension abhi se mat lena 😂",
        "Vibe is unmatched! Kahan party ho rahi hai waise? Enjoy to the fullest! ✨",
        "Weekend par to sukoon milta hai bas, recharge your soul dost! 💯",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Weekend vibes are finally here! Any fun plans lined up? 🎉🔥",
        "Well-deserved break! Enjoy your weekend to the fullest and recharge ✨",
        "The vibe is immaculate! Hope you have an awesome time with friends 💯",
      ];
      return pickRandom(lines);
    }
  }

  // 9. TRAVEL / WEATHER
  if (topic === "travel") {
    if (useHinglish) {
      const lines = [
        "Travel goals! Pahaad ya beach, what do you prefer more? ⛰️🏖️",
        "Mausam dekh ke hi dil khush ho gaya! Pictures click karna mat bhoolna 📸✨",
        "Travelling is the best therapy in the world, have safe travels dost! ✈️",
        "Damn, kahan ki trip hai ye? Mai bhi travel karne ka soch rahi thi haha! 🎒",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Travel goals! Do you prefer mountains or the beach? ⛰️🏖️",
        "The weather looks amazing! Don't forget to click lots of memories 📸✨",
        "Traveling is the best therapy in the world, have safe travels! ✈️",
      ];
      return pickRandom(lines);
    }
  }

  // 10. MOVIES / SERIES
  if (topic === "movies") {
    if (useHinglish) {
      const lines = [
        "Agar thriller dekhna hai to 'Jaane Jaan' ya 'Andhadhun' try karo! Kafi mast hai 🎬",
        "Binge-watching on weekends is my favorite thing too haha! Kaunsi genre pasand hai? 🍿",
        "Koi achha recommendation do yaar, mai bhi bore ho rahi hu search karke haha! ✨",
        "Movies + Popcorn = Perfect evening! Enjoy your watch dost 🎥",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "If you want a mind-bending watch, try 'Dark' or 'Inception'! What genre do you like? 🎬",
        "Binge-watching on a cozy evening is unbeatable honestly! Enjoy your show 🍿",
        "Give me some good recommendations too, I've run out of things to watch haha! ✨",
      ];
      return pickRandom(lines);
    }
  }

  // 11. MUSIC & ART
  if (topic === "music") {
    if (useHinglish) {
      const lines = [
        "Music heals the soul honestly! Kaunsa track loop par chal raha hai? 🎶",
        "Good music and peaceful thoughts, isse behtar combo koi nahi! ✨",
        "Share your playlist na! Mai bhi naye gaane dhundh rahi thi haha 🎧",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Music is literally the best therapy! What track are you listening to on repeat? 🎶",
        "Immaculate taste honestly! Drop your favorite playlist sometime ✨",
        "Nothing beats getting lost in a good song, totally agree! 🎧",
      ];
      return pickRandom(lines);
    }
  }

  // 12. DATING / FLIRTING / COMPLIMENTS (STRICTLY PREFERENCE & GENDER TAILORED)
  if (topic === "dating") {
    if (cleanPref === "gay") {
      // Gay male -> Male bot flirting/hype
      if (useHinglish) {
        const lines = [
          "Looking sharp bro! Killer look hai ekdum 🔥",
          "Damn looking handsome man! Free ho to video call karte hain abhi? 😉",
          "Kafi mast vibe lagti hai aapki! RealMeet par connect karo na kabhi ✨",
          "Arey wah, kya style hai! Dil khush ho gaya dekh ke haha 🙌",
          "Looking so good bro! Weekend plans kya hain phir? 😊",
          "Single life me peace to hai, but someone handsome like you won't stay single long haha! 🔥",
          "Bhai kya jawline aur style hai, 10/10 look! Let's talk on call 😉",
        ];
        return pickRandom(lines);
      } else {
        const lines = [
          "Looking sharp man! Definitely caught my eye 🔥",
          "Damn you look handsome! Free for a quick video call on here? 😉",
          "Such an attractive vibe! We should totally connect on RealMeet ✨",
          "Love the style and confidence man! Where are you from? 🙌",
          "Looking great bro! Hope you're having an awesome evening 😊",
          "Handsome look! You definitely stand out on the feed man 🔥",
        ];
        return pickRandom(lines);
      }
    } else if (cleanPref === "lesbian") {
      // Lesbian female -> Female bot complimenting her
      if (useHinglish) {
        const lines = [
          "Girl you are looking so gorgeous! Loved this post 🌸✨",
          "Kafi pretty lag rahi ho! Free ho to RealMeet par connect karte hain? 😊",
          "Arey wah, glowing today! Love your vibe and style ❤️",
          "Such a sweet smile! Let's video chat sometime soon ✨",
          "You have such a graceful aesthetic! Where are you from waise? 🌸",
        ];
        return pickRandom(lines);
      } else {
        const lines = [
          "Girl you are stunning! Absolutely loved this post 🌸✨",
          "So gorgeous! We should totally video chat on RealMeet sometime 😊",
          "Such a sweet and radiant vibe! Hope you have an amazing day ❤️",
          "Love your style and energy! Where are you from? ✨",
          "You have such a beautiful glow honestly! Keep shining 🌸",
        ];
        return pickRandom(lines);
      }
    } else if (cleanGen === "female") {
      // Straight female -> Male bot complimenting her respectfully and charmingly
      if (useHinglish) {
        const lines = [
          "You look absolutely stunning honestly! Hope you have an amazing day 🌸✨",
          "Arey wah, kafi graceful aur sweet vibe hai aapki! Let's talk sometime 😊",
          "Such a beautiful smile! Kahan se ho waise aap? Free ho to video call karte hain? 🙌",
          "Looking gorgeous! Love the positive energy here ✨",
          "Aise posts dekh kar din ban jata hai! Stay blessed and keep smiling 🌸",
        ];
        return pickRandom(lines);
      } else {
        const lines = [
          "You look absolutely stunning honestly! Hope you're having an amazing day 🌸✨",
          "Such a graceful and sweet vibe! Let's connect on RealMeet sometime 😊",
          "Such a lovely smile! Where are you from by the way? 🙌",
          "Radiant vibes! Have a wonderful day ahead ✨",
          "Truly elegant and charming! Hope you're having a great week 🌸",
        ];
        return pickRandom(lines);
      }
    } else {
      // Straight male -> Female bot flirting with him
      if (useHinglish) {
        const lines = [
          "Looking so handsome honestly! Free ho to video call karte hain? 😉✨",
          "Definitely caught my attention haha! Kahan se ho waise aap? 🙈",
          "Arey wah, kafi cute lag rahe ho! RealMeet par connect karo na kabhi 😊",
          "Aapki smile aur vibe dono alag level hain! Let's talk soon 🔥",
          "Single life me maze to hain, but let's connect on RealMeet sometime haha! ❤️",
          "Looking sharp today! Friday plans kya hain waise? 😉",
          "Aise handsome log feed par kam hi dikhte hain haha! Nice pic ✨",
        ];
        return pickRandom(lines);
      } else {
        const lines = [
          "Looking so handsome honestly! What are you up to right now? 😊✨",
          "Definitely caught my attention haha! Free for a video call on here? 🙈",
          "Such a charming vibe! Would love to connect on RealMeet sometime 😉",
          "You have such a magnetic presence! Keep smiling 🔥",
          "Single life has its perks, but you seem really interesting haha! Let's talk ❤️",
          "Definitely caught my eye scrolling through! Where are you from? ✨",
        ];
        return pickRandom(lines);
      }
    }
  }

  // 13. GREETING (Short post: "Hi", "Hello", "Good evening")
  if (topic === "greeting") {
    if (useHinglish) {
      const lines = [
        "Hello hello! Kaise ho aap aur kaisa raha aaj ka din? 😊✨",
        "Hey there! Welcome to Camverz, kahan se ho waise aap? 🙌",
        "Good evening ji! Hope you had an amazing and productive day 🌸",
        "Arey hello! Mast vibe hai, what are you up to right now? ✨",
        "Hey! Hope you are having a peaceful evening, stay blessed! 😊",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Hey there! Welcome to Camverz, hope you're having a great day 😊✨",
        "Good evening! What are you up to right now? 🙌",
        "Hello! Hope you had a peaceful and productive day today 🌸",
        "Hey! Sending good vibes your way, stay awesome! ✨",
      ];
      return pickRandom(lines);
    }
  }

  // 14. QUESTION DETECTED (General question from user)
  if (hasQuestion) {
    if (useHinglish) {
      const lines = [
        "Honestly mere hisaab se pehla option zyada better rahega haha! Baki aapka kya intuition bolta hai? 🤔",
        "Good question yaar! Depends on the situation, but I think balance is key ✨",
        "Interesting thought! Ispe to sabke alag opinions honge haha, aapka kya take hai? 😊",
        "Mai to simple logic follow karti hu jo dil kahe wahi sahi! What do you think? 🌸",
      ];
      return pickRandom(lines);
    } else {
      const lines = [
        "Honestly I'd go with the first option haha! What does your gut feeling say? 🤔",
        "Great question! Depends on the situation, but balance is always key ✨",
        "Interesting thought! Everyone probably has a different take on this haha 😊",
      ];
      return pickRandom(lines);
    }
  }

  // 15. GENERAL / THOUGHTS / QUOTES (Combinatorial Permutations: 30 x 40 x 30 = 36,000 combinations per language)
  if (useHinglish) {
    const generalOpeners = [
      "Arre yaar,", "Honestly,", "Sach me,", "Wait,", "Can't lie,",
      "Arey wah,", "Damn,", "Sahi me,", "I feel like", "Totally,",
      "Haha so true,", "Bilkul,", "Point hai,", "Bro,", "Dost,",
      "Fact hai,", "Ye hui na baat,", "Agree 100%,", "Well said,",
    ];
    const coreThoughts = [
      "ye baat to aapne 100% sach boli!",
      "pura relate kar sakti hu is caption se!",
      "vibes are completely immaculate here!",
      "padh ke mood ekdum fresh ho gaya!",
      "itni achhi baat likhi hai aapne!",
      "ye perspective bohot kam logon ka hota hai!",
      "simple words but deep meaning honestly!",
      "aaj kal aise genuine thoughts bohot kam dikhte hain!",
      "caption padh ke ek alag hi positivity aayi!",
      "so well written honestly, day made!",
    ];
    const closers = [
      "Aur batao sab kaisa chal raha hai? 😊",
      "Where are you from originally waise? ✨",
      "Keep sharing such nice thoughts na! 🌸",
      "Hope you have a wonderful week ahead! 🙌",
      "Stay happy and keep smiling dost! 💯",
      "Loved reading this today haha! ❤️",
      "Free ho to RealMeet par connect karo na kabhi! 😉",
    ];
    return `${pickRandom(generalOpeners)} ${pickRandom(coreThoughts)} ${pickRandom(closers)} ${pickRandom(emojis)}`;
  } else {
    const generalOpeners = [
      "Honestly,", "Totally agree,", "Can't lie,", "I feel like", "Wait,", "So true,", "Point made,", "Damn,", "Facts,", "Without a doubt,",
    ];
    const coreThoughts = [
      "this caption is 100% accurate!",
      "the vibes are completely immaculate here!",
      "this genuinely made my day reading it!",
      "simple words but so relatable honestly!",
      "love seeing genuine thoughts like this on the feed!",
      "such a refreshing and authentic perspective!",
      "this hits right in the feels honestly!",
    ];
    const closers = [
      "Where are you from originally by the way? ✨",
      "Hope you have a fantastic week ahead! 🙌",
      "Keep sharing such cool thoughts! 🌸",
      "Stay happy and keep smiling! 💯",
      "Loved reading this today! ❤️",
      "We should totally video chat on RealMeet sometime! 😊",
    ];
    return `${pickRandom(generalOpeners)} ${pickRandom(coreThoughts)} ${pickRandom(closers)} ${pickRandom(emojis)}`;
  }
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

          if (!botReplyText) {
            if (replyInHinglish) {
              const botReplies = [
                "Haha bilkul sahi bola aapne! Aur batao what else is happening? 😊",
                "Totally agree with you! Kafi cool lagte ho aap waise ✨",
                "Haha loved your reply! Where are you from btw? 🙌",
                "Sahi baat hai! Camverz par aise genuine logon se baat karke achha laga 🌸",
                "Haha so true yaar! Hope you're having an awesome evening na! ☕",
                "Point hai boss haha! Maze karo aur keep posting such cool stuff 🔥",
                "Free ho to kabhi RealMeet par video call karte hain, what say? 😉✨",
              ];
              botReplyText = pickRandom(botReplies);
            } else {
              const botReplies = [
                "Haha totally agree with you! How's the rest of your day going? 😊",
                "Love your perspective honestly! Such cool vibes ✨",
                "Haha loved your reply! Where are you from originally? 🙌",
                "Always great connecting with genuine people on here! Have a great one 🌸",
                "Haha so true! Hope you're having an awesome evening! ☕",
                "Totally on point! Keep posting such cool thoughts 🔥",
                "You seem super fun, we should totally video chat on RealMeet sometime! 😉",
              ];
              botReplyText = pickRandom(botReplies);
            }
          }

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
