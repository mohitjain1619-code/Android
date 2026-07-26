'use client';
import { useState, useEffect } from 'react';
import { usePathname } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import Navbar from './Navbar';
import Footer from './Footer';
import ParticleBackground from './ParticleBackground';
import LoginModal from './LoginModal';
import OnboardingModal from './OnboardingModal';
import GenderVerificationModal from './GenderVerificationModal';
import AppRedirectModal from './AppRedirectModal';

import { captureFromURL } from '../lib/affiliateTracker';

export default function ClientLayoutWrapper({ children }) {
  const { user, userData, showLogin, setShowLogin, showVerification, setShowVerification, showOnboarding, setShowOnboarding, showAppRedirect } = useAuth();
  const pathname = usePathname();
  const isCallPage = pathname?.startsWith('/call');

  // Capture affiliate referral code if present in URL query
  useEffect(() => {
    const code = captureFromURL();
    if (code) {
      import('../lib/api').then(({ trackAffiliateClick }) => {
        trackAffiliateClick(code, document.referrer, navigator.userAgent)
          .then((res) => console.log('Click tracked successfully:', res))
          .catch((err) => console.error('Failed to track click:', err));
      });
    }
  }, []);

  // Auto-trigger onboarding if user is logged in but profile is not completed
  useEffect(() => {
    if (pathname?.startsWith('/affiliate')) {
      setShowOnboarding(false);
      return;
    }
    if (user && userData && !userData.gender) {
      setShowOnboarding(true);
    } else {
      setShowOnboarding(false);
    }
  }, [user, userData, setShowOnboarding, pathname]);

  return (
    <>
      <div className="bg-gradient-page" />
      <ParticleBackground />
      
      {!isCallPage && <Navbar />}
      
      <main style={{ minHeight: '100vh', position: 'relative', zIndex: 1 }}>
        {children}
      </main>
      
      {!isCallPage && <Footer />}

      {/* Global Modals */}
      {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
      {showOnboarding && <OnboardingModal onClose={() => setShowOnboarding(false)} />}
      {showVerification && <GenderVerificationModal onClose={() => setShowVerification(false)} />}
      {showAppRedirect && <AppRedirectModal />}
    </>
  );
}
