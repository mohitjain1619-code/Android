require("dotenv").config();
const { queryMany } = require("./src/config/database");

async function test() {
  try {
    const sql = `
      SELECT p.id, p.text, u.name, u.gender, u.verified, u.sex_preference 
      FROM posts p
      JOIN users u ON u.id = p.user_id
      LIMIT 5
    `;
    const res = await queryMany(sql);
    console.log("DATABASE RESULTS:", JSON.stringify(res, null, 2));
  } catch (err) {
    console.error("ERROR:", err);
  }
  process.exit(0);
}

test();
