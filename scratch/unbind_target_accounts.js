const path = require('path');
const { Pool } = require(path.resolve(__dirname, '../Backend/ultra-backend/node_modules/pg'));

const pool = new Pool({
  connectionString: "postgresql://camverz:FEJW3XOpa2PAZdiEEiccBqo44dHoIp67@dpg-dapvvs5g1s2s73djjpsg-a.oregon-postgres.render.com/camverz_1h55",
  ssl: { rejectUnauthorized: false }
});

async function unbindAccounts() {
  try {
    console.log("Searching for users mohitjain1619 and monishkarai206...");

    const usersRes = await pool.query(`
      SELECT id, name, email, google_id, created_at 
      FROM users 
      WHERE email ILIKE '%mohitjain1619%' 
         OR email ILIKE '%monishkarai206%'
         OR name ILIKE '%mohitjain1619%'
         OR name ILIKE '%monishkarai206%'
    `);

    console.log("Users found:", usersRes.rows);

    const userIds = usersRes.rows.map(u => u.id);

    if (userIds.length > 0) {
      console.log("\nChecking bound devices in user_devices table for these user IDs:", userIds);
      const devicesRes = await pool.query(
        `SELECT * FROM user_devices WHERE user_id = ANY($1::uuid[])`,
        [userIds]
      );
      console.log(`Bound devices count: ${devicesRes.rows.length}`);
      console.log(devicesRes.rows);

      if (devicesRes.rows.length > 0) {
        console.log("\nUnbinding/Deleting device records for these users...");
        const delRes = await pool.query(
          `DELETE FROM user_devices WHERE user_id = ANY($1::uuid[])`,
          [userIds]
        );
        console.log(`✅ Deleted ${delRes.rowCount} device binding(s) successfully!`);
      } else {
        console.log("No bound devices found for these specific user IDs.");
      }

      // Also check if any device_id linked to these users is bound to other records or email lists
      const deviceIds = devicesRes.rows.map(d => d.device_id).filter(Boolean);
      if (deviceIds.length > 0) {
        console.log("\nAlso clearing device bindings for device_ids:", deviceIds);
        const delDevRes = await pool.query(
          `DELETE FROM user_devices WHERE device_id = ANY($1::text[])`,
          [deviceIds]
        );
        console.log(`✅ Cleaned up total ${delDevRes.rowCount} device binding(s) for those device_ids.`);
      }
    } else {
      console.log("No users matched email/name query.");
    }

    process.exit(0);
  } catch (err) {
    console.error("Error unbinding accounts:", err);
    process.exit(1);
  }
}

unbindAccounts();
