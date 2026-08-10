'use client';
import { useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { setToken } from '../../../lib/api';

function AutologinHandler() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    try {
      const token = searchParams.get('token');
      const redirect = searchParams.get('redirect') || 'profile';

      if (token) {
        setToken(token);
        // Force authentication contexts to reload token from localStorage
        window.dispatchEvent(new Event('storage'));
        router.replace(`/${redirect}`);
      } else {
        router.replace('/');
      }
    } catch (err) {
      console.error('Autologin error:', err);
      router.replace('/');
    }
  }, [searchParams, router]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#060612', color: '#fff', fontFamily: 'sans-serif' }}>
      <div style={{ width: '40px', height: '40px', border: '3px solid rgba(0, 229, 255, 0.1)', borderTopColor: '#00E5FF', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
      <p style={{ marginTop: '16px', color: 'rgba(255, 255, 255, 0.7)' }}>Auto-logging you in securely...</p>
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}} />
    </div>
  );
}

export default function AutologinPage() {
  return (
    <Suspense fallback={
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#060612', color: '#fff', fontFamily: 'sans-serif' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid rgba(0, 229, 255, 0.1)', borderTopColor: '#00E5FF', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
        <p style={{ marginTop: '16px', color: 'rgba(255, 255, 255, 0.7)' }}>Loading session...</p>
      </div>
    }>
      <AutologinHandler />
    </Suspense>
  );
}
