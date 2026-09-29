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
        background: '#040711',
        backgroundImage: 'radial-gradient(circle at center, #0b1736 0%, #030611 100%)',
        color: '#fff',
        fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        padding: '24px',
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
      {/* Central Popup Card */}
      <div 
        onClick={goToPlayStore}
        style={{
          width: '100%',
          maxWidth: '380px',
          background: 'rgba(15, 23, 42, 0.95)',
          border: '2px solid rgba(0, 229, 255, 0.7)',
          borderRadius: '28px',
          padding: '36px 24px',
          textAlign: 'center',
          boxShadow: '0 25px 80px rgba(0, 0, 0, 0.95), 0 0 50px rgba(0, 229, 255, 0.4)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          animation: 'popupFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1)'
        }}
      >
        {/* App Icon */}
        <div style={{
          width: '80px',
          height: '80px',
          borderRadius: '22px',
          background: 'linear-gradient(135deg, #00E5FF, #7C4DFF)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 12px 35px rgba(0, 229, 255, 0.5)',
          marginBottom: '24px'
        }}>
          <span style={{ fontSize: '2.6rem' }}>👑</span>
        </div>

        {/* Modal Title */}
        <h2 style={{ fontSize: '1.4rem', fontWeight: 800, margin: '0 0 12px 0', color: '#fff', letterSpacing: '-0.3px', lineHeight: 1.3 }}>
          For Real & Uninterrupted Fun! 🔥
        </h2>

        {/* Modal Body Message */}
        <p style={{ color: '#94a3b8', fontSize: '0.95rem', lineHeight: 1.55, margin: '0 0 28px 0' }}>
          Please install our official Android Mobile App for smooth 1-on-1 video matching & best experience.
        </p>

        {/* Single OK Button */}
        <button
          onClick={goToPlayStore}
          style={{
            width: '100%',
            background: 'linear-gradient(135deg, #00E5FF, #00B0FF)',
            color: '#000',
            border: 'none',
            padding: '16px',
            borderRadius: '50px',
            fontSize: '1.1rem',
            fontWeight: 800,
            boxShadow: '0 8px 30px rgba(0, 229, 255, 0.6)',
            cursor: 'pointer',
            letterSpacing: '0.5px'
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
          from { opacity: 0; transform: scale(0.88); }
          to { opacity: 1; transform: scale(1); }
        }
      `}} />
    </div>
  );
}

export default function DownloadRedirectPage() {
  return (
    <Suspense fallback={
      <div style={{ position: 'fixed', inset: 0, zIndex: 2147483647, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#030611', color: '#fff' }}>
        <p>Loading...</p>
      </div>
    }>
      <DownloadRedirectContent />
    </Suspense>
  );
}

