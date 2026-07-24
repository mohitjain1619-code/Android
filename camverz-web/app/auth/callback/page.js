'use client';
import { useEffect } from 'react';

export default function AuthCallback() {
  useEffect(() => {
    try {
      // Parse the hash parameters from URL (e.g. #iss=...&id_token=...)
      const hash = window.location.hash;
      if (hash) {
        const params = new URLSearchParams(hash.substring(1));
        const idToken = params.get('id_token');
        
        if (idToken && window.opener) {
          // Send token back to the parent window (auth-context.js handleMessage listener)
          window.opener.postMessage(
            { type: 'google-auth', idToken },
            window.location.origin
          );
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
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}} />
    </div>
  );
}
