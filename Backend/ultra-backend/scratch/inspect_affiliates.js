const { queryMany } = require('../src/config/database');

async function inspect() {
  try {
    const affiliates = await queryMany('SELECT * FROM affiliates');
    console.log('--- Affiliates ---');
    console.log(affiliates);

    const clicks = await queryMany('SELECT * FROM affiliate_clicks');
    console.log('\n--- Clicks ---');
    console.log(clicks);

    const signups = await queryMany('SELECT * FROM affiliate_signups');
    console.log('\n--- Signups ---');
    console.log(signups);
  } catch (err) {
    console.error('Error:', err);
  }
}

inspect();
