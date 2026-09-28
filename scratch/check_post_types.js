const path = require('path');
const { queryMany } = require(path.resolve(__dirname, '../Backend/ultra-backend/src/config/database'));

async function test() {
  try {
    console.log("--- DISTINCT TYPES IN COMMUNITY_POSTS ---");
    const types = await queryMany(`SELECT DISTINCT type FROM community_posts`);
    console.log("Distinct types:", types);

    console.log("\n--- COMMUNITY POSTS GROUPED BY USER & TYPE ---");
    const commSample = await queryMany(`SELECT user_id, type, COUNT(*)::int as count FROM community_posts GROUP BY user_id, type`);
    console.log(commSample);

    console.log("\n--- STORIES GROUPED BY USER ---");
    const storySample = await queryMany(`SELECT user_id, COUNT(*)::int as count FROM stories GROUP BY user_id`);
    console.log(storySample);

    console.log("\n--- FEED POSTS GROUPED BY USER ---");
    const feedSample = await queryMany(`SELECT user_id, COUNT(*)::int as count FROM posts GROUP BY user_id`);
    console.log(feedSample);

    process.exit(0);
  } catch (err) {
    console.error("Error:", err);
    process.exit(1);
  }
}

test();
