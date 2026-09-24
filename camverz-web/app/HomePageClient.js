'use client';
import { useState, useEffect, useRef, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { Video, Shield, Users, Globe, Heart, Zap, ArrowRight, Star, Lock, Sparkles, MessageSquare, UserCheck, Smartphone, QrCode, AlertCircle, Download } from 'lucide-react';
import styles from './page.module.css';

function HomeContent() {
  const { user, userData, loading, setShowLogin, setShowOnboarding } = useAuth();
  const searchParams = useSearchParams();
  const router = useRouter();
  const [selectedCategory, setSelectedCategory] = useState(null);
  const featuresRef = useRef(null);
  const statsRef = useRef(null);

  useEffect(() => {
    if (searchParams.get('login') === 'true' && !user) setShowLogin(true);
  }, [searchParams, user, setShowLogin]);

  // GSAP Scroll Animations
  useEffect(() => {
    let ctx;
    let isMounted = true;
    const initGsap = async () => {
      try {
        const gsapModule = await import('gsap');
        const scrollTriggerModule = await import('gsap/ScrollTrigger');
        
        if (!isMounted) return;

        const gsapInstance = gsapModule.gsap;
        gsapInstance.registerPlugin(scrollTriggerModule.ScrollTrigger);

        ctx = gsapInstance.context(() => {
          // Animate feature cards
          gsapInstance.utils.toArray(`.${styles.featureCard}`).forEach((card, i) => {
            gsapInstance.from(card, {
              scrollTrigger: { trigger: card, start: 'top 85%', toggleActions: 'play none none none' },
              y: 40, opacity: 0, duration: 0.6, delay: i * 0.1, ease: 'power2.out',
            });
          });

          // Animate stats
          gsapInstance.utils.toArray(`.${styles.statItem}`).forEach((stat, i) => {
            gsapInstance.from(stat, {
              scrollTrigger: { trigger: stat, start: 'top 90%' },
              y: 30, opacity: 0, duration: 0.5, delay: i * 0.15, ease: 'power2.out',
            });
          });

          // Animate preference cards immediately on load
          gsapInstance.utils.toArray(`.${styles.prefCard}`).forEach((card, i) => {
            gsapInstance.from(card, {
              scale: 0.9, opacity: 0, duration: 0.5, delay: i * 0.1, ease: 'back.out(1.4)',
            });
          });
        });

        // Force refresh ScrollTrigger positions after layout stabilizes
        setTimeout(() => {
          if (isMounted) scrollTriggerModule.ScrollTrigger.refresh();
        }, 200);
        setTimeout(() => {
          if (isMounted) scrollTriggerModule.ScrollTrigger.refresh();
        }, 700);
      } catch (e) {
        // GSAP not critical
      }
    };
    initGsap();

    return () => {
      isMounted = false;
      if (ctx) ctx.revert();
    };
  }, []);

  const isProfileIncomplete = (uData) => {
    if (!uData) return true;
    const g = (uData.gender || '').trim().toLowerCase();
    return !g || g === 'unspecified' || g === '';
  };

  const handleStartCall = (category = 'straight') => {
    if (!user) {
      setShowLogin(true);
      return;
    }
    if (isProfileIncomplete(userData)) {
      setShowOnboarding(true);
      return;
    }

    const userGender = (userData.gender || '').trim().toLowerCase();
    const reqCategory = (category || 'straight').trim().toLowerCase();

    if (reqCategory === 'lesbian' && userGender === 'male') {
      alert("⚠️ Restricted Category\n\nLesbian category is for Female members only. Male members can connect via Straight or Gay video chat.");
      return;
    }

    if (reqCategory === 'gay' && userGender === 'female') {
      alert("⚠️ Restricted Category\n\nGay category is for Male members only. Female members can connect via Straight or Lesbian video chat.");
      return;
    }

    router.push(`/call?category=${category}`);
  };

  const features = [
    { icon: Sparkles, title: 'Real Meet & Party Host', desc: 'Host or discover real-world meetups, house parties, coffee hangouts, and events with real-time join requests & party approvals.' },
    { icon: Video, title: 'Instant 1-on-1 Video Match', desc: 'Get matched instantly in live face-to-face video calls with Straight, Gay, or Lesbian orientation filtering.' },
    { icon: Users, title: 'Stories & Community Feed', desc: 'Post daily stories, share social updates, like & comment on community posts, and engage with verified creators.' },
    { icon: UserCheck, title: 'Verified Profiles & Anti-Catfish', desc: 'Gender-verified members and AI profile checks ensure 100% authentic real-people connections with no fake bots.' },
    { icon: MessageSquare, title: 'Private Direct Messaging', desc: 'Send direct messages, coordinate party invites, and keep in touch with your matches through real-time chat.' },
    { icon: Lock, title: 'Strict Zero-Nudity & P2P Privacy', desc: '24/7 AI moderation, screenshot blocking, and encrypted peer-to-peer WebRTC calls with zero server recordings.' },
  ];

  return (
    <div className={styles.page}>
      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <Zap size={14} />
            <span>Social Media & Live Connection Network</span>
          </div>
          <h1 className={styles.heroTitle}>
            Connect & Socialize<br />
            Through <span className="neon-text">Live Video Chat</span>
          </h1>
          <p className={styles.heroDesc}>
            Discover new friends, join inclusive LGBTQ+ social circles, share stories, and build real-time connections worldwide through instant 1-on-1 video chat.
          </p>
          <div className={styles.heroBtns}>
            <button className="btn-neon" onClick={() => handleStartCall('straight')}>
              <Video size={18} /> Start Calling <ArrowRight size={16} />
            </button>
            <a href="#features" className="btn-glass">Learn More</a>
          </div>
        </div>
        <div className={styles.heroVisual}>
          <div className={styles.glowOrb1} />
          <div className={styles.glowOrb2} />
          <div className={styles.floatingCard}>
            <div className={styles.cardAvatar}>👤</div>
            <div className={styles.cardPulse} />
            <span>Connecting...</span>
          </div>
        </div>
      </section>

      {/* Preference Cards */}
      <section className={styles.preferences} id="categories">
        <div className="section">
          <div className="section-title">
            <h2>Choose Your Preference</h2>
            <p>Select a category to get matched with the right people</p>
          </div>
          <div className={styles.prefGrid}>
            {[
              { key: 'straight', emoji: '💑', label: 'Straight', desc: 'Male & Female matching', color: '#FF006E' },
              { key: 'gay', emoji: '👨‍❤️‍👨', label: 'Gay', desc: 'Male only matching', color: '#2979FF' },
              { key: 'lesbian', emoji: '👩‍❤️‍👩', label: 'Lesbian', desc: 'Female only matching', color: '#BD00FF' },
            ].map(cat => (
              <button
                key={cat.key}
                className={styles.prefCard}
                onClick={() => handleStartCall(cat.key)}
                style={{ '--card-color': cat.color }}
              >
                <span className={styles.prefEmoji}>{cat.emoji}</span>
                <h3>{cat.label}</h3>
                <p>{cat.desc}</p>
                <div className={styles.prefArrow}><ArrowRight size={20} /></div>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Mobile App Download Section */}
      <section className={styles.appDownloadSection} id="mobile-app">
        <div className="section">
          <div className={styles.appBannerCard}>
            <div className={styles.appBannerGlow} />
            
            <div className={styles.appBannerContent}>
              <div className={styles.appBadge}>
                <Smartphone size={15} />
                <span>Camverz Android App</span>
              </div>
              
              <h2 className={styles.appTitle}>
                Find People <span className="neon-text">Faster & Better</span> On Mobile App
              </h2>
              
              <p className={styles.appSubtitle}>
                In our Mobile App, you find people much faster! Get instant push alerts, zero-lag video calls, real-world party check-ins, and camera optimizations designed for real-time socializing on the go.
              </p>

              <div className={styles.appFeaturesList}>
                <div className={styles.appFeatureItem}>
                  <Zap size={16} className={styles.appIconCyan} />
                  <span><strong>Instant Match Alerts:</strong> Never miss a video call or RealMeet party request</span>
                </div>
                <div className={styles.appFeatureItem}>
                  <Sparkles size={16} className={styles.appIconMagenta} />
                  <span><strong>Smooth HD Video Calls:</strong> Optimized mobile WebRTC engine for low latency</span>
                </div>
                <div className={styles.appFeatureItem}>
                  <Smartphone size={16} className={styles.appIconGreen} />
                  <span><strong>RealMeet Party Host:</strong> Discover nearby events or host house parties on the go</span>
                </div>
              </div>

              {/* Highlight Note for Subscriptions */}
              <div className={styles.subscriptionAlertBox}>
                <AlertCircle size={22} className={styles.alertIcon} />
                <div>
                  <strong>📌 IMPORTANT NOTE FOR SUBSCRIPTIONS:</strong>
                  <p>For any kind of subscription, VIP plan upgrade, or coin top-ups, <strong>you have to do it from the website only</strong>.</p>
                </div>
              </div>

              <div className={styles.appButtonsRow}>
                <a 
                  href="https://play.google.com/store/apps/details?id=com.mohit.camverz" 
                  target="_blank" 
                  rel="noopener noreferrer" 
                  className={styles.playStoreBtn}
                >
                  <svg viewBox="0 0 512 512" width="24" height="24" fill="currentColor">
                    <path d="M325.8 253.9L80.9 10.1C75.2 4.1 67.2 0 58.1 0 40 0 25.3 14.7 25.3 32.8c0 3.3.5 6.5 1.5 9.5l299 211.6zM25.3 479.2C25.3 497.3 40 512 58.1 512c9.1 0 17.1-4.1 22.8-10.1l244.9-243.8L26.8 469.7c-1 3-1.5 6.2-1.5 9.5zM468.9 220.8L360.7 160l-45.7 45.7 45.7 45.7 108.2-60.8c8.9-5.1 14.1-14.7 14.1-24.9s-5.2-19.8-14.1-24.9zM25.3 54.1v403.8l202.9-201.9L25.3 54.1z"/>
                  </svg>
                  <div className={styles.btnText}>
                    <span className={styles.smallText}>GET IT ON</span>
                    <span className={styles.bigText}>Google Play</span>
                  </div>
                </a>
              </div>
            </div>

            <div className={styles.appQrWrapper}>
              <div className={styles.qrCard}>
                <div className={styles.qrHeader}>
                  <QrCode size={18} />
                  <span>Scan to Install App</span>
                </div>
                <div className={styles.qrFrame}>
                  <img 
                    src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=https://play.google.com/store/apps/details?id=com.mohit.camverz&color=000000&bgcolor=ffffff" 
                    alt="Scan QR Code to Download Camverz Android App" 
                    className={styles.qrImage}
                  />
                </div>
                <p className={styles.qrDesc}>Scan with your phone camera to download Android App instantly</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className={styles.features} id="features" ref={featuresRef}>
        <div className="section">
          <div className="section-title">
            <h2>Why Choose Camverz?</h2>
            <p>Built for real connections, not superficial swipes</p>
          </div>
          <div className={styles.featureGrid}>
            {features.map((f, i) => (
              <div key={i} className={styles.featureCard}>
                <div className={styles.featureIcon}><f.icon size={24} /></div>
                <h3>{f.title}</h3>
                <p>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className={styles.cta}>
        <div className="section" style={{ textAlign: 'center' }}>
          <h2>Ready to Connect Worldwide?</h2>
          <p>Join thousands of people meeting authentically every single day.</p>
          <button className="btn-neon" onClick={() => handleStartCall('straight')}>
            <Video size={18} /> Get Started Now <ArrowRight size={16} />
          </button>
        </div>
      </section>
    </div>
  );
}

export default function HomePageClient() {
  return (
    <Suspense fallback={
      <div style={{ minHeight: '100vh', minWidth: '950px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#060612', color: 'rgba(255, 255, 255, 0.7)' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid rgba(0, 229, 255, 0.1)', borderTopColor: '#00E5FF', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
        <style dangerouslySetInnerHTML={{__html: `
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}} />
      </div>
    }>
      <HomeContent />
    </Suspense>
  );
}
