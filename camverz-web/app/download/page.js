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

    // Handle user tap anywhere on page
    const handleGlobalClick = (e) => {
      // If user clicked web button specifically, route to web app
      if (e.target && e.target.closest && e.target.closest('#web-btn')) {
        router.push('/call');
        return;
      }

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
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at center, #0f172a 0%, #060612 100%)',
        color: '#fff',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        padding: '24px',
        textAlign: 'center',
        cursor: 'pointer',
        userSelect: 'none'
      }}
    >
      {/* App Badge Header */}
      <div style={{
        width: '84px',
        height: '84px',
        borderRadius: '20px',
        background: 'linear-gradient(135deg, #00E5FF, #7C4DFF)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 12px 35px rgba(0, 229, 255, 0.35)',
        marginBottom: '20px'
      }}>
        <span style={{ fontSize: '2.5rem' }}>📹</span>
      </div>

      <h1 style={{ fontSize: '1.8rem', fontWeight: 800, margin: '0 0 8px 0', background: 'linear-gradient(135deg, #fff, #94a3b8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
        Camverz App
      </h1>

      <p style={{ color: '#94a3b8', fontSize: '0.95rem', maxWidth: '360px', margin: '0 0 28px 0', lineHeight: 1.5 }}>
        Instant Live Video Matching, Real Meet & Social Communities
      </p>

      {/* Main Download CTA Button */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          triggerDownload();
        }}
        style={{
          background: 'linear-gradient(135deg, #00E5FF, #00B0FF)',
          color: '#000',
          border: 'none',
          padding: '16px 32px',
          borderRadius: '50px',
          fontSize: '1.05rem',
          fontWeight: 700,
          boxShadow: '0 8px 25px rgba(0, 229, 255, 0.4)',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          marginBottom: '16px',
          transition: 'transform 0.2s ease'
        }}
      >
        <span style={{ fontSize: '1.3rem' }}>▶</span>
        <span>Download Official Android App</span>
      </button>

      <p style={{ fontSize: '0.78rem', color: '#64748b', margin: '0 0 24px 0' }}>
        👇 Tap anywhere on screen to open Google Play Store
      </p>

      {/* Web Option */}
      <button
        id="web-btn"
        onClick={(e) => {
          e.stopPropagation();
          router.push('/call');
        }}
        style={{
          background: 'rgba(255,255,255,0.06)',
          color: '#cbd5e1',
          border: '1px solid rgba(255,255,255,0.12)',
          padding: '10px 20px',
          borderRadius: '30px',
          fontSize: '0.85rem',
          fontWeight: 600,
          cursor: 'pointer'
        }}
      >
        🌐 Open Web App Instantly
      </button>
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
