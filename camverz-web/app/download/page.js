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

  useEffect(() => {
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

    // Handle user tap anywhere on screen
    const handleGlobalClick = () => {
      if (androidUser && storeUrl) {
        window.location.href = storeUrl;
      } else {
        router.push('/');
      }
    };

    window.addEventListener('click', handleGlobalClick);
    return () => {
      window.removeEventListener('click', handleGlobalClick);
    };
  }, [ref, router]);

  const triggerDownload = () => {
    if (isAndroid && playStoreUrl) {
      window.location.href = playStoreUrl;
    } else {
      router.push('/');
    }
  };

  return (
    <div 
      onClick={triggerDownload}
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
      {/* Central Uncloseable Popup Card */}
      <div 
        onClick={triggerDownload}
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

        {/* Single OK / Download Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            triggerDownload();
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
            animation: 'btnGlow 2s infinite alternate'
          }}
        >
          OK (OPEN PLAY STORE)
        </button>
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
