'use client';
import { useEffect, useState } from 'react';

export default function AuthCallback() {
  const [showFallback, setShowFallback] = useState(false);

  useEffect(() => {
    try {
      // Parse the hash parameters from URL (e.g. #iss=...&id_token=...)
      const hash = window.location.hash;
      if (hash) {
        const params = new URLSearchParams(hash.substring(1));
        const idToken = params.get('id_token');

        if (idToken) {
          try {
            localStorage.setItem('google_auth_token_temp', idToken);
          } catch (e) {
            console.error('Failed to set localStorage token fallback:', e);
          }

          if (window.opener) {
            // Send token back to the parent window (auth-context.js handleMessage listener)
            window.opener.postMessage(
              { type: 'google-auth', idToken },
              window.location.origin
            );

            // Auto-close popup window after a short delay.
            setTimeout(() => {
              window.close();
              setTimeout(() => setShowFallback(true), 500);
            }, 300);
          } else {
            // Full-tab redirect fallback for mobile browsers — return to home page
            window.location.href = '/';
          }
        }
      }
    } catch (err) {
      console.error('Error during OAuth callback:', err);
    }
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#060612', color: '#fff', fontFamily: 'sans-serif' }}>
      <div style={{ width: '40px', height: '40px', border: '3px solid rgba(0, 229, 255, 0.1)', borderTopColor: '#00E5FF', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
      <p style={{ marginTop: '16px', color: 'rgba(255, 255, 255, 0.7)' }}>Completing Google sign in...</p>
      {showFallback && (
        <button
          onClick={() => window.close()}
          style={{
            marginTop: '16px',
            padding: '10px 24px',
            background: 'rgba(0, 229, 255, 0.15)',
            color: '#00E5FF',
            border: '1px solid rgba(0, 229, 255, 0.3)',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '0.9rem'
          }}
        >
          Click here to close this window
        </button>
      )}
      <style dangerouslySetInnerHTML={{
        __html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}} />
    </div>
  );
}
