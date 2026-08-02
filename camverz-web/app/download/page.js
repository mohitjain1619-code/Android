'use client';

import { useEffect, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { captureReferral } from '../../lib/affiliateTracker';
import { trackAffiliateClick } from '../../lib/api';

function DownloadRedirectContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const ref = searchParams.get('ref');

  useEffect(() => {
    const destinationCode = ref ? ref.trim().toUpperCase() : '';

    // 1. Capture referral code if present
    if (destinationCode) {
      captureReferral(destinationCode);
      // Track the click in the background
      trackAffiliateClick(destinationCode, document.referrer, navigator.userAgent)
        .catch(err => console.error("Click tracking failed:", err));
    }

    // 2. Detect User Agent
    const ua = navigator.userAgent || navigator.vendor || window.opera;
    const isIOS = /iPad|iPhone|iPod/.test(ua) && !window.MSStream;
    const isAndroid = /android/i.test(ua);

    // 3. Resolve destination
    if (isAndroid) {
      // Redirect to Google Play Store with referrerr
      const playStoreUrl = `https://play.google.com/store/apps/details?id=com.mohitt.camverz&referrer=${destinationCode}`;
      window.location.href = playStoreUrl;
    } else {
      // Redirect iOS or Desktop users to the website main page
      const webUrl = destinationCode ? `/?ref=${destinationCode}` : '/';
      router.replace(webUrl);
    }
  }, [ref, router]);

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#060612',
      color: '#fff',
      fontFamily: 'sans-serif'
    }}>
      <div style={{
        width: '40px',
        height: '40px',
        border: '3px solid rgba(0, 229, 255, 0.1)',
        borderTopColor: '#00E5FF',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
        marginBottom: '20px'
      }} />
      <style dangerouslySetInnerHTML={{
        __html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}} />
      <h3 style={{ margin: 0, fontWeight: 600 }}>Redirecting you to the app...</h3>
      <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: '0.85rem', marginTop: '8px' }}>
        Please wait a moment.
      </p>
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
