'use client';

import { useEffect, useState, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { captureReferral } from '../../lib/affiliateTracker';
import { trackAffiliateClick } from '../../lib/api';

function DownloadRedirectContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const ref = searchParams.get('ref');
  const [playStoreUrl, setPlayStoreUrl] = useState('');
  const [isAndroid, setIsAndroid] = useState(true);
  const [hasVisitedPlayStore, setHasVisitedPlayStore] = useState(false);

  useEffect(() => {
    // Check if user has already visited Play Store in this session
    try {
      const visited = sessionStorage.getItem('visited_playstore') === 'true';
      setHasVisitedPlayStore(visited);
    } catch (e) {}

    const destinationCode = ref ? ref.trim().toUpperCase() : '';

    // Detect User Agent
    const ua = navigator.userAgent || navigator.vendor || window.opera;
    const androidUser = /android/i.test(ua);
    setIsAndroid(androidUser);

    const storeUrl = `https://play.google.com/store/apps/details?id=com.mohitt.camverz${destinationCode ? `&referrer=${destinationCode}` : ''}`;
    setPlayStoreUrl(storeUrl);

    if (destinationCode) {
      // 1. Capture referral code locally
      captureReferral(destinationCode);
      
      // 2. Track click asynchronously in background
      trackAffiliateClick(destinationCode, document.referrer, navigator.userAgent).catch(err => {
        console.error("Click tracking error:", err);
      });
    }
  }, [ref]);

  const triggerPlayStoreRedirect = () => {
    // Mark that user is visiting Play Store now
    try {
      sessionStorage.setItem('visited_playstore', 'true');
    } catch (e) {}
    setHasVisitedPlayStore(true);

    if (isAndroid && playStoreUrl) {
      window.location.href = playStoreUrl;
    } else {
      router.push('/call');
    }
  };

  const handleContinueToWeb = (e) => {
    e.stopPropagation();
    router.push('/call');
  };

  return (
    <div 
      onClick={() => {
        if (!hasVisitedPlayStore) {
          triggerPlayStoreRedirect();
        }
      }}
      style={{
        minHeight: '100vh',
        width: '100vw',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'rgba(2, 2, 8, 0.96)',
        backdropFilter: 'blur(30px)',
        WebkitBackdropFilter: 'blur(30px)',
        color: '#fff',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        padding: '20px',
        boxSizing: 'border-box',
        position: 'fixed',
        top: 0,
        left: 0,
        zIndex: 99999,
        userSelect: 'none',
        cursor: 'pointer'
      }}
    >
      {/* Central Popup Card */}
      <div 
        onClick={(e) => {
          e.stopPropagation();
          if (!hasVisitedPlayStore) {
            triggerPlayStoreRedirect();
          }
        }}
        style={{
          width: '100%',
          maxWidth: '380px',
          background: '#0b1120',
          border: '1.5px solid rgba(0, 229, 255, 0.6)',
          borderRadius: '28px',
          padding: '36px 26px',
          textAlign: 'center',
          boxShadow: '0 30px 90px rgba(0, 0, 0, 0.95), 0 0 60px rgba(0, 229, 255, 0.35)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          animation: 'popupFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
          position: 'relative'
        }}
      >
        {/* App Icon */}
        <div style={{
          width: '76px',
          height: '76px',
          borderRadius: '20px',
          background: 'linear-gradient(135deg, #00E5FF, #7C4DFF)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 12px 30px rgba(0, 229, 255, 0.45)',
          marginBottom: '22px'
        }}>
          <span style={{ fontSize: '2.4rem' }}>📱</span>
        </div>

        {/* Modal Title */}
        <h2 style={{ fontSize: '1.45rem', fontWeight: 800, margin: '0 0 12px 0', color: '#fff', letterSpacing: '-0.3px' }}>
          Better Experience on Mobile App 👑
        </h2>

        {/* Modal Body Message */}
        <p style={{ color: '#94a3b8', fontSize: '0.94rem', lineHeight: 1.55, margin: '0 0 28px 0' }}>
          For faster 1-on-1 video matching, HD video calls, and seamless social features, please use our official Android app.
        </p>

        {/* Primary Play Store Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            triggerPlayStoreRedirect();
          }}
          style={{
            width: '100%',
            background: 'linear-gradient(135deg, #00E5FF, #00B0FF)',
            color: '#000',
            border: 'none',
            padding: '16px',
            borderRadius: '50px',
            fontSize: '1.08rem',
            fontWeight: 800,
            boxShadow: '0 8px 30px rgba(0, 229, 255, 0.5)',
            cursor: 'pointer',
            letterSpacing: '0.5px',
            animation: 'btnGlow 2s infinite alternate',
            marginBottom: hasVisitedPlayStore ? '12px' : '0'
          }}
        >
          OK
        </button>

        {/* Continue to Web Button (ONLY SHOWN AFTER USER RETURNS FROM PLAY STORE) */}
        {hasVisitedPlayStore && (
          <button
            onClick={handleContinueToWeb}
            style={{
              width: '100%',
              background: 'rgba(255,255,255,0.06)',
              color: '#cbd5e1',
              border: '1px solid rgba(255,255,255,0.15)',
              padding: '12px',
              borderRadius: '50px',
              fontSize: '0.88rem',
              fontWeight: 600,
              cursor: 'pointer',
              marginTop: '4px'
            }}
          >
            🌐 Continue to Web Version
          </button>
        )}
      </div>

      <style dangerouslySetInnerHTML={{
        __html: `
        @keyframes popupFadeIn {
          from { opacity: 0; transform: scale(0.88); }
          to { opacity: 1; transform: scale(1); }
        }
        @keyframes btnGlow {
          from { boxShadow: 0 8px 25px rgba(0, 229, 255, 0.4); }
          to { boxShadow: 0 12px 40px rgba(0, 229, 255, 0.8); }
        }
      `}} />
    </div>
  );
}

export default function DownloadRedirectPage() {
  return (
    <Suspense fallback={
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#060612', color: '#fff' }}>
        <p>Loading...</p>
      </div>
    }>
      <DownloadRedirectContent />
    </Suspense>
  );
}
