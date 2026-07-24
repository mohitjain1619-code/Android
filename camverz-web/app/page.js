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
    if (loading) return;
    if (!user) { setShowLogin(true); return; }
    if (!userData?.gender) {
      setShowOnboarding(true);
      return;
    }

    if (category === 'gay' && userData.gender !== 'male') {
      alert('Only males can join the Gay section'); return;
    }
    if (category === 'lesbian' && userData.gender !== 'female') {
      alert('Only females can join the Lesbian section'); return;
    }
    router.push(`/call?category=${category}`);
  };

  const features = [
    { icon: Video, title: 'Random Video Calls', desc: 'Get matched with real people for live video conversations worldwide.' },
    { icon: Shield, title: 'Gender Verified', desc: 'All female users are verified to ensure authentic connections.' },
    { icon: Users, title: 'Community', desc: 'Join a growing community of like-minded people seeking real connections.' },
    { icon: Globe, title: 'Global Reach', desc: 'Connect with people from over 190 countries around the world.' },
    { icon: Heart, title: 'Dating & Friends', desc: 'Find dates, make friends, or just have fun conversations.' },
    { icon: Lock, title: 'Safe & Private', desc: 'Your calls are secure. No recordings, no screenshots.' },
  ];

  return (
    <div className={styles.page}>
      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <Zap size={14} />
            <span>Live Video Calling Platform</span>
          </div>
          <h1 className={styles.heroTitle}>
            Meet Real People<br />
            Through <span className="neon-text">Video Calls</span>
          </h1>
          <p className={styles.heroDesc}>
            Get matched randomly and start video calling. Find dates, make friends, or just chat with strangers from around the world.
          </p>
          <div className={styles.heroBtns}>
            <button className="btn-neon" onClick={() => user ? router.push('/call') : setShowLogin(true)}>
              <Video size={18} /> Start Calling <ArrowRight size={16} />
            </button>
            <a href="#features" className="btn-glass">Learn More</a>
          </div>
          <div className={styles.heroStats}>
            <div><strong>50K+</strong><span>Active Users</span></div>
            <div className={styles.divider} />
            <div><strong>1M+</strong><span>Video Calls</span></div>
            <div className={styles.divider} />
            <div><strong>190+</strong><span>Countries</span></div>
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

      {/* Stats */}
      <section className={styles.stats} ref={statsRef}>
        <div className="section">
          <div className={styles.statsGrid}>
            {[
              { value: '50K+', label: 'Active Users', icon: Users },
              { value: '1M+', label: 'Video Calls Made', icon: Video },
              { value: '190+', label: 'Countries', icon: Globe },
              { value: '4.8', label: 'App Rating', icon: Star },
            ].map((s, i) => (
              <div key={i} className={styles.statItem}>
                <s.icon size={24} className={styles.statIcon} />
                <div className={styles.statValue}>{s.value}</div>
                <div className={styles.statLabel}>{s.label}</div>
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
          <button className="btn-neon" onClick={() => user ? router.push('/call') : setShowLogin(true)} style={{ fontSize: '1.1rem', padding: '14px 36px' }}>
            <Video size={20} /> Start Video Calling
          </button>
        </div>
      </section>

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
