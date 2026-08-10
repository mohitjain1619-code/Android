'use client';
import { useState, useEffect, useRef, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { Video, Shield, Users, Globe, Heart, Zap, ArrowRight, Star, Lock } from 'lucide-react';
import styles from './page.module.css';

function HomeContent() {
  const { user, userData, loading, setShowLogin, setShowOnboarding } = useAuth();
  const searchParams = useSearchParams();
  const router = useRouter();
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [showLaunchModal, setShowLaunchModal] = useState(false);
  const [timeLeft, setTimeLeft] = useState({ days: 0, hours: 0, minutes: 0, seconds: 0 });
  const featuresRef = useRef(null);
  const statsRef = useRef(null);

  const TESTER_EMAILS = [
    'jainmohit.cr007@gmail.com',
    'mohitj8120@gmail.com',
    'monishkarai206@gmail.com',
    'mohitjain1619@gmail.com',
    'info.meetblis@gmail.com',
    'wetviapp@gmail.com',
    'mohitissuingthis@gmail.com'
  ];

  useEffect(() => {
    if (loading) return;
    if (searchParams.get('launch') === 'true') {
      const email = user?.email || userData?.email;
      const isTester = email && TESTER_EMAILS.map(e => e.toLowerCase().trim()).includes(email.toLowerCase().trim());
      if (!isTester) {
        setShowLaunchModal(true);
      }
      router.replace('/');
    }
  }, [searchParams, router, user, userData, loading]);

  useEffect(() => {
    // Launch Date: August 15, 2026 00:00:00 IST
    const launchDate = new Date("August 15, 2026 00:00:00 GMT+0530").getTime();
    
    const updateCountdown = () => {
      const now = new Date().getTime();
      const difference = launchDate - now;

      if (difference <= 0) {
        setTimeLeft({ days: 0, hours: 0, minutes: 0, seconds: 0 });
        return;
      }

      const days = Math.floor(difference / (1000 * 60 * 60 * 24));
      const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((difference % (1000 * 60)) / 1000);

      setTimeLeft({ days, hours, minutes, seconds });
    };

    updateCountdown();
    const interval = setInterval(updateCountdown, 1000);
    return () => clearInterval(interval);
  }, []);

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

          // Animate preference cards immediately on load (without ScrollTrigger)
          // Since the page is scaled down on mobile, these are visible on load and ScrollTrigger can miss them
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

  const handleCategoryClick = (category) => {
    // 15th August launch check commented out so users can video call directly
    router.push(`/call?category=${category}`);
    /*
    const email = user?.email || userData?.email;
    const isTester = email && TESTER_EMAILS.map(e => e.toLowerCase().trim()).includes(email.toLowerCase().trim());
    if (isTester) {
      router.push(`/call?category=${category}`);
    } else {
      setShowLaunchModal(true);
    }
    */
  };

  const features = [
    { icon: Video, title: 'Random Video Calls', desc: 'Get matched with real people for live video conversations worldwide.' },
    { icon: Shield, title: 'Gender Verified', desc: 'All female users are verified to ensure authentic connections.' },
    { icon: Users, title: 'Community', desc: 'Join a growing community of like-minded people seeking real connections.' },
    { icon: Globe, title: 'Global Reach', desc: 'Connect with people from over 190 countries around the world.' },
    // { icon: Heart, title: 'Dating & Friends', desc: 'Find dates, make friends, or just have fun conversations.' },
    { icon: Heart, title: 'Language & Networking', desc: 'Practice languages, build digital networks, or have interactive live discussions.' },
    { icon: Lock, title: 'Safe & Private', desc: 'Your calls are secure. No recordings, no screenshots.' },
  ];

  return (
    <div className={styles.page}>
      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <Zap size={14} />
            {/* <span>Live Video Calling Platform</span> */}
            <span>Virtual Networking & SaaS Community</span>
          </div>
          <h1 className={styles.heroTitle}>
            Meet Verified Peers<br />
            Through <span className="neon-text">Virtual Networking</span>
          </h1>
          {/* <p className={styles.heroDesc}>Get matched randomly and start video calling. Find dates, make friends, or just chat with strangers from around the world.</p> */}
          <p className={styles.heroDesc}>
            Connect with verified members worldwide for live virtual networking, skill sharing, language exchange, and digital collaboration.
          </p>
          <div className={styles.heroBtns}>
            <button className="btn-neon" onClick={() => {
              // 15th August launch check commented out
              router.push('/call');
              /*
              const email = user?.email || userData?.email;
              const isTester = email && TESTER_EMAILS.map(e => e.toLowerCase().trim()).includes(email.toLowerCase().trim());
              if (isTester) {
                router.push('/call');
              } else {
                setShowLaunchModal(true);
              }
              */
            }}>
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
                onClick={() => handleCategoryClick(cat.key)}
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
          <h2>Ready to Meet Someone New?</h2>
          <p style={{ color: 'var(--text-secondary)', margin: '12px 0 24px', maxWidth: 500, marginInline: 'auto' }}>
            Join thousands of people who are already making real connections through video calls.
          </p>
          <button className="btn-neon" onClick={() => setShowLaunchModal(true)} style={{ fontSize: '1.1rem', padding: '14px 36px' }}>
            <Video size={20} /> Start Video Calling
          </button>
        </div>
      </section>

      {/* Custom Launch Modal */}
      {showLaunchModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          zIndex: 9999,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'rgba(5, 5, 12, 0.85)',
          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          padding: '20px',
          animation: 'modalFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards'
        }} onClick={() => setShowLaunchModal(false)}>
          <style dangerouslySetInnerHTML={{__html: `
            @keyframes modalFadeIn {
              from { opacity: 0; }
              to { opacity: 1; }
            }
            @keyframes contentScaleUp {
              from { transform: scale(0.92) translateY(10px); opacity: 0; }
              to { transform: scale(1) translateY(0); opacity: 1; }
            }
            @keyframes tricolorGlow {
              0%, 100% { border-color: rgba(255, 103, 31, 0.25); box-shadow: 0 0 25px rgba(255, 103, 31, 0.1); }
              33% { border-color: rgba(255, 255, 255, 0.25); box-shadow: 0 0 25px rgba(255, 255, 255, 0.1); }
              66% { border-color: rgba(18, 136, 37, 0.25); box-shadow: 0 0 25px rgba(18, 136, 37, 0.1); }
            }
            .modal-content-box {
              background: rgba(15, 15, 25, 0.9);
              border: 1px solid rgba(255, 255, 255, 0.08);
              border-radius: 24px;
              width: 100%;
              max-width: 500px;
              padding: 40px;
              text-align: center;
              position: relative;
              animation: contentScaleUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards, tricolorGlow 8s infinite alternate;
            }
            .countdown-segment {
              background: rgba(255, 255, 255, 0.03);
              border: 1px solid rgba(255, 255, 255, 0.05);
              border-radius: 12px;
              padding: 12px 10px;
              min-width: 70px;
            }
            .countdown-value {
              font-size: 1.8rem;
              font-weight: 800;
              font-family: var(--font-display);
              background: linear-gradient(135deg, #FF671F, #FFFFFF, #128837);
              -webkit-background-clip: text;
              -webkit-text-fill-color: transparent;
            }
            .countdown-label {
              font-size: 0.7rem;
              color: var(--text-muted);
              text-transform: uppercase;
              letter-spacing: 0.05em;
              margin-top: 4px;
              font-weight: 600;
            }
          `}} />
          <div className="modal-content-box" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', fontSize: '2.5rem', marginBottom: '20px' }}>
              <span>🇮🇳</span>
              <span style={{ animation: 'float 3s ease-in-out infinite' }}>🚀</span>
            </div>

            <span style={{
              background: 'rgba(255, 103, 31, 0.1)',
              border: '1px solid rgba(255, 103, 31, 0.25)',
              color: '#FF671F',
              fontSize: '0.75rem',
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.1em',
              padding: '4px 12px',
              borderRadius: '50px',
              display: 'inline-block',
              marginBottom: '16px'
            }}>
              Grand Opening
            </span>

            <h2 style={{
              fontSize: '2rem',
              fontWeight: 800,
              color: '#fff',
              marginBottom: '12px',
              lineHeight: 1.2,
              fontFamily: 'var(--font-display)'
            }}>
              15 August 2026
            </h2>

            <p style={{
              color: 'var(--text-secondary)',
              fontSize: '0.95rem',
              lineHeight: 1.6,
              marginBottom: '32px',
              padding: '0 10px'
            }}>
              Camverz is gearing up for a spectacular launch on India's Independence Day! We are preparing the ultimate, ultra-high-definition random video calling experience. Stay tuned!
            </p>

            {/* Countdown Grid */}
            <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginBottom: '32px' }}>
              <div className="countdown-segment">
                <div className="countdown-value">{timeLeft.days}</div>
                <div className="countdown-label">Days</div>
              </div>
              <div className="countdown-segment">
                <div className="countdown-value">{timeLeft.hours.toString().padStart(2, '0')}</div>
                <div className="countdown-label">Hours</div>
              </div>
              <div className="countdown-segment">
                <div className="countdown-value">{timeLeft.minutes.toString().padStart(2, '0')}</div>
                <div className="countdown-label">Mins</div>
              </div>
              <div className="countdown-segment">
                <div className="countdown-value">{timeLeft.seconds.toString().padStart(2, '0')}</div>
                <div className="countdown-label">Secs</div>
              </div>
            </div>

            <button
              className="btn-neon"
              onClick={() => setShowLaunchModal(false)}
              style={{
                width: '100%',
                padding: '14px',
                fontSize: '1rem',
                fontWeight: 600,
                background: 'linear-gradient(135deg, #FF671F, #128837)',
                boxShadow: '0 4px 15px rgba(255, 103, 31, 0.25)',
                border: 'none',
                cursor: 'pointer',
                color: '#fff',
                borderRadius: '8px'
              }}
            >
              Jai Hind! I'll Wait 🇮🇳
            </button>
          </div>
        </div>
      )}

    </div>
  );
}

export default function HomePage() {
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
