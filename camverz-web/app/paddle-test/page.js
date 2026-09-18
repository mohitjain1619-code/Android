'use client';

import { useState, useEffect } from 'react';
import { initializePaddle } from '@paddle/paddle-js';
import { CreditCard, ShieldCheck, Zap, RefreshCw, Terminal, CheckCircle2 } from 'lucide-react';
import styles from './page.module.css';

export default function PaddleTestPage() {
  const [token, setToken] = useState(
    process.env.NEXT_PUBLIC_PADDLE_CLIENT_TOKEN || 'test_0123456789abcdef0123456789'
  );
  const [priceId, setPriceId] = useState(
    process.env.NEXT_PUBLIC_PADDLE_PRICE_ID || 'pri_01h123456789abcdef01234567'
  );
  const [environment, setEnvironment] = useState(
    process.env.NEXT_PUBLIC_PADDLE_ENV || 'sandbox'
  );

  const [paddleInstance, setPaddleInstance] = useState(null);
  const [loading, setLoading] = useState(false);
  const [logs, setLogs] = useState([]);

  const addLog = (msg) => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs((prev) => [`[${timestamp}] ${msg}`, ...prev]);
  };

  // Initialize Paddle SDK on token / environment change
  useEffect(() => {
    let isMounted = true;
    if (!token || token.startsWith('test_0123456789')) {
      addLog('⚠️ Using placeholder token. Update token field or .env.local to load actual Paddle credentials.');
      return;
    }

    setLoading(true);
    addLog(`Initializing Paddle (${environment})...`);

    initializePaddle({
      token,
      environment: environment === 'sandbox' ? 'sandbox' : 'production',
      eventCallback: (data) => {
        if (!isMounted) return;
        addLog(`Event: ${data.name || 'Checkout Action'}`);
        console.log('Paddle Event:', data);
      },
    })
      .then((instance) => {
        if (isMounted && instance) {
          setPaddleInstance(instance);
          setLoading(false);
          addLog('✅ Paddle SDK initialized successfully!');
        }
      })
      .catch((err) => {
        if (isMounted) {
          setLoading(false);
          addLog(`❌ Paddle Init Error: ${err.message || err}`);
          console.error('Paddle Error:', err);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [token, environment]);

  const handleCheckout = () => {
    if (!priceId || priceId.startsWith('pri_01h123456789')) {
      addLog('⚠️ Please enter a valid Paddle Price ID (pri_...) from your dashboard.');
    }

    if (paddleInstance) {
      addLog(`Opening overlay checkout for Price ID: ${priceId}`);
      try {
        paddleInstance.Checkout.open({
          items: [{ priceId: priceId, quantity: 1 }],
        });
      } catch (err) {
        addLog(`❌ Checkout Trigger Error: ${err.message || err}`);
      }
    } else {
      addLog('⌛ Paddle instance not ready yet. Retrying initialization...');
      // Direct CDN fallback attempt if NPM instance is pending
      if (typeof window !== 'undefined' && window.Paddle) {
        window.Paddle.Environment.set(environment);
        window.Paddle.Initialize({
          token,
          eventCallback: (data) => addLog(`Event: ${data.name}`),
        });
        window.Paddle.Checkout.open({ items: [{ priceId, quantity: 1 }] });
      } else {
        alert('Paddle is still initializing or token is invalid. Please check your credentials.');
      }
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.glow} />

      <div className={styles.header}>
        <div className={styles.badge}>
          <Zap size={14} />
          <span>Paddle Payment Test Page</span>
        </div>
        <h1 className={styles.title}>Paddle Integration Test</h1>
        <p className={styles.subtitle}>
          Isolated sandbox testing page for Camverz Paddle Billing. Razorpay pricing on the live site remains completely unaffected.
        </p>
      </div>

      <div className={styles.card}>
        <div className={styles.cardHeader}>
          <div className={styles.planTitle}>Camverz VIP Test Pass</div>
          <span
            className={`${styles.envTag} ${
              environment === 'sandbox' ? styles.envSandbox : styles.envLive
            }`}
          >
            {environment}
          </span>
        </div>

        <div className={styles.priceContainer}>
          <span className={styles.price}>$5.00</span>
          <span className={styles.pricePeriod}>/ 24 Hours Pass</span>
        </div>

        <div className={styles.configSection}>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Paddle Client Token:</label>
            <input
              type="text"
              className={styles.input}
              value={token}
              onChange={(e) => setToken(e.target.value.trim())}
              placeholder="test_... or live_..."
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label}>Paddle Price ID:</label>
            <input
              type="text"
              className={styles.input}
              value={priceId}
              onChange={(e) => setPriceId(e.target.value.trim())}
              placeholder="pri_..."
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label}>Environment:</label>
            <select
              className={styles.input}
              value={environment}
              onChange={(e) => setEnvironment(e.target.value)}
            >
              <option value="sandbox">Sandbox (Testing)</option>
              <option value="live">Live (Production)</option>
            </select>
          </div>
        </div>

        <button
          className={styles.payButton}
          onClick={handleCheckout}
          disabled={loading}
        >
          {loading ? (
            <>
              <RefreshCw size={18} className="animate-spin" />
              <span>Initializing Paddle...</span>
            </>
          ) : (
            <>
              <CreditCard size={18} />
              <span>Test Paddle Checkout ($5.00)</span>
            </>
          )}
        </button>
      </div>

      <div className={styles.logsBox}>
        <div className={styles.logsHeader}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Terminal size={14} />
            <span>Event &amp; Status Logs</span>
          </div>
          <button
            onClick={() => setLogs([])}
            style={{
              background: 'none',
              border: 'none',
              color: '#64748B',
              cursor: 'pointer',
              fontSize: '0.75rem',
            }}
          >
            Clear
          </button>
        </div>
        <div className={styles.logsList}>
          {logs.length === 0 ? (
            <div style={{ color: '#64748B' }}>No events logged yet. Click checkout to test.</div>
          ) : (
            logs.map((log, idx) => (
              <div key={idx} className={styles.logEntry}>
                {log}
              </div>
            ))
          )}
        </div>
      </div>

      <div className={styles.helperBox}>
        <strong style={{ color: '#00E5FF' }}>💡 How to Test:</strong>
        <ul style={{ margin: '8px 0 0 16px', padding: 0 }}>
          <li>Enter your Sandbox Client Token (`test_...`) and Price ID (`pri_...`) above or in `.env.local`.</li>
          <li>Click "Test Paddle Checkout" to open Paddle's Overlay Checkout modal.</li>
          <li>Use Paddle Sandbox test cards (e.g., card number `4242 4242 4242 4242`, any future expiration date, any CVV).</li>
        </ul>
      </div>
    </div>
  );
}
