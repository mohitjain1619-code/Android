const { Pool } = require("pg");

const pool = new Pool({
  host: process.env.POSTGRES_HOST || "localhost",
  port: parseInt(process.env.POSTGRES_PORT || "5432"),
  database: process.env.POSTGRES_DB || "camverz",
  user: process.env.POSTGRES_USER || "camverz",
  password: process.env.POSTGRES_PASSWORD || "camverz_dev_2024",
});

async function main() {
  try {
    const res = await pool.query("SELECT id, name, email, city, created_at FROM users ORDER BY created_at DESC");
    console.log("--- QUERY SUCCESSFUL ---");
    console.log(`Total users registered: ${res.rows.length}`);
    console.log(JSON.stringify(res.rows, null, 2));
  } catch (err) {
    console.error("--- QUERY FAILED ---");
    console.error(err);
  } finally {
    await pool.end();
  }
}
main();
