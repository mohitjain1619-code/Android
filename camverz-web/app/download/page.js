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

    // Detect User Agent
    const ua = navigator.userAgent || navigator.vendor || window.opera;
    const isAndroid = /android/i.test(ua);

    const handleRedirect = () => {
      if (isAndroid) {
        // Redirect to Google Play Store with referrer
        const playStoreUrl = `https://play.google.com/store/apps/details?id=com.mohitt.camverz&referrer=${destinationCode}`;
        window.location.href = playStoreUrl;
      } else {
        // Redirect iOS or Desktop users to the website main page (referral is already stored in localStorage)
        router.replace('/');
      }
    };

    if (destinationCode) {
      // 1. Capture referral code locally
      captureReferral(destinationCode);
      
      // 2. Track click and wait for response before routing to ensure browser sends request
      trackAffiliateClick(destinationCode, document.referrer, navigator.userAgent)
        .then(() => {
          console.log("Click tracked successfully");
          handleRedirect();
        })
        .catch(err => {
          console.error("Click tracking failed:", err);
          handleRedirect(); // Redirect anyway on error
        });
    } else {
      handleRedirect();
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
