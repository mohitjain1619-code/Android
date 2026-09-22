'use client';
import { useState, useEffect, useRef, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { Video, Shield, Users, Globe, Heart, Zap, ArrowRight, Star, Lock, Sparkles, MessageSquare, UserCheck } from 'lucide-react';
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
            <button className="btn-neon" onClick={() => router.push('/call')}>
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
          <h2>Ready to Connect Worldwide?</h2>
          <p>Join thousands of people meeting authentically every single day.</p>
          <button className="btn-neon" onClick={() => router.push('/call')}>
            <Video size={18} /> Get Started Now <ArrowRight size={16} />
          </button>
        </div>
      </section>

      {/* Launch Countdown Modal */}
      {showLaunchModal && (
        <div 
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 99999,
            backgroundColor: 'rgba(5, 5, 15, 0.85)',
            backdropFilter: 'blur(12px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
          }}
        >
          <div 
            style={{
              background: 'linear-gradient(135deg, rgba(20, 20, 35, 0.95), rgba(10, 10, 20, 0.98))',
              border: '1px solid rgba(0, 229, 255, 0.3)',
              boxShadow: '0 0 50px rgba(0, 229, 255, 0.2)',
              borderRadius: '24px',
              padding: '40px 30px',
              maxWidth: '480px',
              width: '100%',
              textAlign: 'center',
              color: '#fff'
            }}
          >
            <div style={{ fontSize: '3.5rem', marginBottom: '15px' }}>🚀</div>
            <h2 style={{ fontSize: '1.8rem', fontWeight: '800', marginBottom: '10px', background: 'linear-gradient(90deg, #00E5FF, #BD00FF)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Camverz is Launching Soon!
            </h2>
            <p style={{ color: 'rgba(255, 255, 255, 0.7)', fontSize: '0.95rem', marginBottom: '25px', lineHeight: '1.5' }}>
              We are fine-tuning the platform for the ultimate video call experience. Camverz officially goes live on <strong>Independence Day, 15th August 2026</strong>!
            </p>

            {/* Countdown Box */}
            <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginBottom: '30px' }}>
              {[
                { label: 'Days', val: timeLeft.days },
                { label: 'Hours', val: timeLeft.hours },
                { label: 'Mins', val: timeLeft.minutes },
                { label: 'Secs', val: timeLeft.seconds }
              ].map((item, idx) => (
                <div key={idx} style={{ background: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '12px', padding: '10px 14px', minWidth: '65px' }}>
                  <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#00E5FF' }}>{String(item.val).padStart(2, '0')}</div>
                  <div style={{ fontSize: '0.7rem', color: 'rgba(255, 255, 255, 0.5)', textTransform: 'uppercase', marginTop: '2px' }}>{item.label}</div>
                </div>
              ))}
            </div>

            <button 
              onClick={() => setShowLaunchModal(false)}
              className="btn-neon"
              style={{
                width: '100%',
                padding: '14px',
                fontSize: '1rem',
                fontWeight: '600',
                cursor: 'pointer',
                background: 'linear-gradient(90deg, #00E5FF, #BD00FF)',
                border: 'none',
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
