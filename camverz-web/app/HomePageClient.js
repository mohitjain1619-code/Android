'use client';
import { useState, useEffect, useRef, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { Video, Shield, Users, Globe, Heart, Zap, ArrowRight, Star, Lock, Sparkles, MessageSquare, UserCheck, Smartphone, QrCode, AlertCircle, Download, MapPin, Flame } from 'lucide-react';
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
    { icon: Sparkles, title: 'RealMeet & Party Host', desc: 'Host or join real-world dates, nightlife events, private house parties, and club hangouts with instant join requests.' },
    { icon: Video, title: 'Instant Random Video Match', desc: 'Tap once to connect live 1-on-1 with strangers. Filter by Straight (opposite gender), Gay, or Lesbian matching.' },
    { icon: Heart, title: 'Fantasy & Open Dating', desc: 'Explore bold desires, secret fantasies, and connect discreetly with open-minded adults in your city.' },
    { icon: UserCheck, title: 'Verified Profiles & Anti-Catfish', desc: 'Face gesture verification ensures 100% genuine girls and boys on camera. Zero bots, zero fake profiles.' },
    { icon: Users, title: 'Stories & Community Feed', desc: 'Post daily stories, share photos, like and comment on community posts, and chat directly with verified members.' },
    { icon: Lock, title: '18+ Private & Anti-Screenshot', desc: 'Hardware-level screenshot and screen recording blocking with peer-to-peer WebRTC encryption for complete privacy.' },
  ];

  return (
    <div className={styles.page}>
      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <Zap size={14} />
            <span>🔥 18+ Live Random Video Chat & Real Meet Dating</span>
          </div>
          <h1 className={styles.heroTitle}>
            Live Random Video Chat<br />
            & <span className="neon-text">RealMeet Party Dating</span>
          </h1>
          <p className={styles.heroDesc}>
            Instant 1-on-1 video calls with verified girls & boys worldwide. Plan real-world dates, attend wild house parties, and explore open fantasies — 100% anti-screenshot safe.
          </p>
          <div className={styles.heroBtns}>
            <button className="btn-neon" onClick={() => handleStartCall('straight')}>
              <Video size={18} /> Random Video Match <ArrowRight size={16} />
            </button>
            <Link href="/realmeet?tab=party" className="btn-glass" style={{ borderColor: 'rgba(255, 0, 110, 0.4)', color: '#FF006E' }}>
              <Flame size={18} /> Real House Parties
            </Link>
            <Link href="/realmeet?tab=meet" className="btn-glass" style={{ borderColor: 'rgba(0, 229, 255, 0.4)', color: '#00E5FF' }}>
              <MapPin size={18} /> Real Meet Dating
            </Link>
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
            <p>Instant 1-on-1 live video match with real people</p>
          </div>
          <div className={styles.prefGrid}>
            {[
              { key: 'straight', emoji: '💑', label: 'Straight', desc: 'Girls & Boys 1-on-1 Video Match', color: '#FF006E' },
              { key: 'gay', emoji: '👨‍❤️‍👨', label: 'Gay', desc: 'Male 1-on-1 Video Chat & Dating', color: '#2979FF' },
              { key: 'lesbian', emoji: '👩‍❤️‍👩', label: 'Lesbian', desc: 'Female 1-on-1 Video Chat & Dating', color: '#BD00FF' },
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

      {/* Real House Party & Real Meet Showcase */}
      <section className={styles.showcaseSection} id="realmeet-modes">
        <div className="section">
          <div className="section-title">
            <span className={styles.showcaseBadge}>🔥 OFFLINE &amp; REAL WORLD</span>
            <h2>Real House Parties &amp; Real Meet Dating</h2>
            <p>Don&apos;t just talk on camera — take your connection to the real world</p>
          </div>

          <div className={styles.showcaseGrid}>
            {/* Mode 1: Real House Party */}
            <div className={`${styles.showcaseCard} ${styles.cardParty}`}>
              <div className={styles.showcaseCardGlow} />
              <div className={styles.showcaseHeader}>
                <div className={styles.showcaseIconWrap} style={{ background: 'rgba(255, 0, 110, 0.15)', color: '#FF006E' }}>
                  <Flame size={28} />
                </div>
                <span className={styles.showcasePill} style={{ background: 'rgba(255, 0, 110, 0.2)', color: '#FF006E' }}>
                  🎉 PARTY HOST
                </span>
              </div>
              <h3>Real House Parties &amp; Nightlife</h3>
              <p>
                Host private house parties, villa pool bashes, rooftop mixers, or nightlife clubbing in your city. Review guest join requests, approve who enters, and coordinate BYOB and vibes.
              </p>
              <ul className={styles.perkList}>
                <li><span>👑</span> Host Approvals &amp; Verified Guest List</li>
                <li><span>📍</span> Secret Venue Address Revealed on Approval</li>
                <li><span>🍾</span> BYOB, Music, Pool Parties &amp; Club Passes</li>
              </ul>
              <Link href="/realmeet?tab=party" className={`${styles.showcaseBtn} ${styles.btnParty}`}>
                Explore Real House Parties <ArrowRight size={16} />
              </Link>
            </div>

            {/* Mode 2: Real Meet */}
            <div className={`${styles.showcaseCard} ${styles.cardMeet}`}>
              <div className={styles.showcaseCardGlow} />
              <div className={styles.showcaseHeader}>
                <div className={styles.showcaseIconWrap} style={{ background: 'rgba(0, 229, 255, 0.15)', color: '#00E5FF' }}>
                  <MapPin size={28} />
                </div>
                <span className={styles.showcasePill} style={{ background: 'rgba(0, 229, 255, 0.2)', color: '#00E5FF' }}>
                  ☕ 1-ON-1 CITY DATING
                </span>
              </div>
              <h3>Real Meet 1-on-1 Hangouts</h3>
              <p>
                Take connection from screen to real life. Post or discover 1-on-1 city hangouts: coffee dates, drinks, dinner, late-night drives, or casual weekend exploration with face-verified singles.
              </p>
              <ul className={styles.perkList}>
                <li><span>💑</span> 1-on-1 Offline Dating with Verified Profiles</li>
                <li><span>🚫</span> Anti-Catfish: Face-Verified Members Only</li>
                <li><span>💬</span> Direct Private Requests &amp; Real-Time Chats</li>
              </ul>
              <Link href="/realmeet?tab=meet" className={`${styles.showcaseBtn} ${styles.btnMeet}`}>
                Find Real Meet Dates <ArrowRight size={16} />
              </Link>
            </div>

            {/* Mode 3: Fantasy Meetups */}
            <div className={`${styles.showcaseCard} ${styles.cardFantasy}`}>
              <div className={styles.showcaseCardGlow} />
              <div className={styles.showcaseHeader}>
                <div className={styles.showcaseIconWrap} style={{ background: 'rgba(189, 0, 255, 0.15)', color: '#BD00FF' }}>
                  <Sparkles size={28} />
                </div>
                <span className={styles.showcasePill} style={{ background: 'rgba(189, 0, 255, 0.2)', color: '#BD00FF' }}>
                  🔮 SECRET FANTASY
                </span>
              </div>
              <h3>Fantasy &amp; Secret Desires</h3>
              <p>
                Connect discreetly with open-minded adults who share your exact desires. No judgment, no superficial fluff — express your wild fantasies and connect with like-minded people safely.
              </p>
              <ul className={styles.perkList}>
                <li><span>🔒</span> 100% Private, Safe &amp; Discreet Matchmaking</li>
                <li><span>⚡</span> Open-Minded Adults &amp; Bold Desires</li>
                <li><span>🛡️</span> Hardware-Level Anti-Screenshot Security</li>
              </ul>
              <Link href="/realmeet?tab=fantasy" className={`${styles.showcaseBtn} ${styles.btnFantasy}`}>
                Explore Fantasy Meetups <ArrowRight size={16} />
              </Link>
            </div>
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
                Match & Video Call <span className="neon-text">Faster On Mobile</span>
              </h2>

              <p className={styles.appSubtitle}>
                Download the Camverz Android App for instant push alerts, zero-lag HD video calls, nearby RealMeet dating, house party invites, and camera optimizations on the go.
              </p>

              <div className={styles.appFeaturesList}>
                <div className={styles.appFeatureItem}>
                  <Zap size={16} className={styles.appIconCyan} />
                  <span><strong>Instant Match Alerts:</strong> Never miss a live video call or RealMeet party request</span>
                </div>
                <div className={styles.appFeatureItem}>
                  <Sparkles size={16} className={styles.appIconMagenta} />
                  <span><strong>Smooth HD Video Calls:</strong> Low-latency 1-on-1 random video matching</span>
                </div>
                <div className={styles.appFeatureItem}>
                  <Smartphone size={16} className={styles.appIconGreen} />
                  <span><strong>RealMeet & Party Host:</strong> Discover nearby dates, club parties, or host your own</span>
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
                    <path d="M325.8 253.9L80.9 10.1C75.2 4.1 67.2 0 58.1 0 40 0 25.3 14.7 25.3 32.8c0 3.3.5 6.5 1.5 9.5l299 211.6zM25.3 479.2C25.3 497.3 40 512 58.1 512c9.1 0 17.1-4.1 22.8-10.1l244.9-243.8L26.8 469.7c-1 3-1.5 6.2-1.5 9.5zM468.9 220.8L360.7 160l-45.7 45.7 45.7 45.7 108.2-60.8c8.9-5.1 14.1-14.7 14.1-24.9s-5.2-19.8-14.1-24.9zM25.3 54.1v403.8l202.9-201.9L25.3 54.1z" />
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
            <h2>Why Camverz?</h2>
            <p>Real face-to-face random video calls, real-world dates, and wild parties</p>
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
          <h2>Ready to Meet Someone Hot?</h2>
          <p>Join thousands of real adults video calling, dating, and partying every single day.</p>
          <button className="btn-neon" onClick={() => handleStartCall('straight')}>
            <Video size={18} /> Start Video Match <ArrowRight size={16} />
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
        <style dangerouslySetInnerHTML={{
          __html: `
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
