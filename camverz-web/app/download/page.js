'use client';

import { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { captureReferral } from '../../lib/affiliateTracker';
import { trackAffiliateClick } from '../../lib/api';

function DownloadRedirectContent() {
  const searchParams = useSearchParams();
  const ref = searchParams.get('ref');
  const [playStoreUrl, setPlayStoreUrl] = useState('');

  useEffect(() => {
    const destinationCode = ref ? ref.trim().toUpperCase() : '';
    const storeUrl = `https://play.google.com/store/apps/details?id=com.mohitt.camverz${destinationCode ? `&referrer=${destinationCode}` : ''}`;
    setPlayStoreUrl(storeUrl);

    if (destinationCode) {
      captureReferral(destinationCode);
      trackAffiliateClick(destinationCode, document.referrer, navigator.userAgent).catch(err => {
        console.error("Click tracking error:", err);
      });
    }
  }, [ref]);

  const goToPlayStore = () => {
    const url = playStoreUrl || 'https://play.google.com/store/apps/details?id=com.mohitt.camverz';
    window.location.href = url;
  };

  return (
    <div 
      onClick={goToPlayStore}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        width: '100vw',
        height: '100vh',
        background: '#02040a',
        backgroundImage: 'radial-gradient(circle at 50% 40%, #0d1e4a 0%, #02040a 100%)',
        color: '#fff',
        fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        padding: '20px',
        boxSizing: 'border-box',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 2147483647,
        userSelect: 'none',
        WebkitUserSelect: 'none',
        touchAction: 'none',
        cursor: 'pointer',
        overflow: 'hidden'
      }}
    >
      {/* Interstitial Ad Container */}
      <div 
        onClick={goToPlayStore}
        style={{
          width: '92%',
          maxWidth: '430px',
          background: 'rgba(11, 18, 35, 0.98)',
          border: '2px solid rgba(0, 229, 255, 0.75)',
          borderRadius: '32px',
          padding: '40px 28px',
          textAlign: 'center',
          boxShadow: '0 30px 100px rgba(0, 0, 0, 0.98), 0 0 65px rgba(0, 229, 255, 0.45)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          animation: 'popupFadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
          boxSizing: 'border-box'
        }}
      >
        {/* Top Premium Badge */}
        <div style={{
          background: 'rgba(0, 229, 255, 0.12)',
          border: '1px solid rgba(0, 229, 255, 0.4)',
          borderRadius: '50px',
          padding: '6px 16px',
          color: '#00E5FF',
          fontSize: '0.78rem',
          fontWeight: 800,
          letterSpacing: '1px',
          textTransform: 'uppercase',
          marginBottom: '24px'
        }}>
          ⚡ Official Mobile App Required
        </div>

        {/* Big Icon */}
        <div style={{
          width: '90px',
          height: '90px',
          borderRadius: '26px',
          background: 'linear-gradient(135deg, #00E5FF 0%, #7C4DFF 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 16px 40px rgba(0, 229, 255, 0.5)',
          marginBottom: '26px'
        }}>
          <span style={{ fontSize: '3rem' }}>👑</span>
        </div>

        {/* Headline */}
        <h2 style={{ fontSize: '1.6rem', fontWeight: 900, margin: '0 0 14px 0', color: '#ffffff', letterSpacing: '-0.4px', lineHeight: 1.25 }}>
          FOR REAL & UNINTERRUPTED FUN! 🔥
        </h2>

        {/* Body Message */}
        <p style={{ color: '#94a3b8', fontSize: '1rem', lineHeight: 1.6, margin: '0 0 32px 0', fontWeight: 500 }}>
          Please download our official Android App from Google Play Store to enjoy 1-on-1 video calls & premium features.
        </p>

        {/* OK Button */}
        <button
          onClick={goToPlayStore}
          style={{
            width: '100%',
            background: 'linear-gradient(135deg, #00E5FF 0%, #00B0FF 100%)',
            color: '#000',
            border: 'none',
            padding: '18px',
            borderRadius: '50px',
            fontSize: '1.25rem',
            fontWeight: 900,
            boxShadow: '0 10px 35px rgba(0, 229, 255, 0.6)',
            cursor: 'pointer',
            letterSpacing: '0.6px',
            animation: 'btnPulse 1.8s infinite alternate'
          }}
        >
          OK
        </button>
      </div>

      <style dangerouslySetInnerHTML={{
        __html: `
        html, body {
          overflow: hidden !important;
        }
        @keyframes popupFadeIn {
          from { opacity: 0; transform: scale(0.85); }
          to { opacity: 1; transform: scale(1); }
        }
        @keyframes btnPulse {
          from { transform: scale(1); boxShadow: 0 10px 35px rgba(0, 229, 255, 0.5); }
          to { transform: scale(1.02); boxShadow: 0 14px 45px rgba(0, 229, 255, 0.85); }
        }
      `}} />
    </div>
  );
}

export default function DownloadRedirectPage() {
  return (
    <Suspense fallback={
      <div style={{ position: 'fixed', inset: 0, zIndex: 2147483647, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#02040a', color: '#fff' }}>
        <p>Loading...</p>
      </div>
    }>
      <DownloadRedirectContent />
    </Suspense>
  );
}


