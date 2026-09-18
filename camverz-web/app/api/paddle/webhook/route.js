import { NextResponse } from 'next/server';
import crypto from 'crypto';

/**
 * Paddle Webhook Handler
 * Route: /api/paddle/webhook
 * Handled Events: transaction.completed, subscription.created, subscription.updated, etc.
 */
export async function POST(req) {
  try {
    const rawBody = await req.text();
    const signatureHeader = req.headers.get('paddle-signature');

    // 1. Signature Check (if secret is configured)
    const webhookSecret = process.env.PADDLE_WEBHOOK_SECRET;
    if (webhookSecret && signatureHeader) {
      const isValid = verifyPaddleSignature(rawBody, signatureHeader, webhookSecret);
      if (!isValid) {
        console.error('❌ Invalid Paddle webhook signature');
        return NextResponse.json({ error: 'Invalid signature' }, { status: 401 });
      }
    }

    // 2. Parse Event Data
    let event;
    try {
      event = JSON.parse(rawBody);
    } catch (e) {
      return NextResponse.json({ error: 'Invalid JSON payload' }, { status: 400 });
    }

    const { event_type, data } = event;
    console.log(`🔔 Received Paddle Webhook Event: ${event_type}`, data?.id);

    // 3. Process Events
    switch (event_type) {
      case 'transaction.completed':
        console.log(`✅ Transaction Completed: ${data?.id}, Amount: ${data?.details?.totals?.total}`);
        // TODO: Grant user pass/access based on custom_data or customer_id
        break;

      case 'subscription.created':
        console.log(`🎉 Subscription Created: ${data?.id}, Customer: ${data?.customer_id}`);
        break;

      case 'subscription.canceled':
        console.log(`⚠️ Subscription Canceled: ${data?.id}`);
        break;

      default:
        console.log(`ℹ️ Unhandled event type: ${event_type}`);
    }

    return NextResponse.json({ success: true, event_type });
  } catch (error) {
    console.error('❌ Error handling Paddle webhook:', error);
    return NextResponse.json({ error: 'Internal Server Error' }, { status: 500 });
  }
}

/**
 * Verifies Paddle Billing (v2) signature header (ts=...;h=...)
 */
function verifyPaddleSignature(rawBody, signatureHeader, secretKey) {
  try {
    const parts = signatureHeader.split(';');
    let ts = '';
    let h = '';

    for (const part of parts) {
      const [key, value] = part.split('=');
      if (key === 'ts') ts = value;
      if (key === 'h') h = value;
    }

    if (!ts || !h) return false;

    const payload = `${ts}:${rawBody}`;
    const hmac = crypto.createHmac('sha256', secretKey).update(payload).digest('hex');

    return crypto.timingSafeEqual(Buffer.from(hmac), Buffer.from(h));
  } catch (err) {
    console.error('Signature verification exception:', err);
    return false;
  }
}
