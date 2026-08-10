'use client';
import { Shield, Video, Users, Heart, Sparkles, Globe } from 'lucide-react';
import styles from './page.module.css';

export default function AboutPage() {
  const values = [
    {
      icon: Heart,
      title: 'Real Authenticity',
      desc: 'We believe face-to-face video conversations foster true connection. No fake profiles, no filters, and no endless messaging queues. What you see is what you get.'
    },
    {
      icon: Shield,
      title: 'Safety First',
      desc: 'We are committed to creating a secure and respectful environment. Through AI-powered gender verification, proactive user reporting, and active moderation, we keep bad actors away.'
    },
    {
      icon: Globe,
      title: 'Borderless Connections',
      desc: 'Our platform matches people across continents. We break down cultural and geographical walls, allowing you to discover friendships and relationships globally.'
    }
  ];

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Hero Section */}
        <section className={styles.hero}>
          <div className={styles.badge}>
            <Sparkles size={14} />
            <span>Connecting the World</span>
          </div>
          <h1 className="neon-text">About Camverz</h1>
          <p className={styles.subtitle}>
            We are redefining how people meet in the digital age by bringing back spontaneous, face-to-face conversations.
          </p>
        </section>

        {/* Story Section */}
        <section className={styles.storyCard}>
          <h2>Our Story</h2>
          {/*
          <p>
            In a world saturated with superficial dating profiles, messaging delays, and catfishing, we wanted to build something simpler and more human. Camverz was born out of a desire to replicate the excitement of real-world spontaneous encounters.
          </p>
          <p>
            Whether you want to learn a new language, share a laugh with someone from a different continent, make life-long friends, or find a romantic partner, Camverz provides a safe, simple, and exciting platform to match and chat instantly.
          </p>
          */}
          <p>
            Camverz was created to empower professionals, creators, and individuals across the globe to connect through live virtual networking, real-time video sessions, and interactive social communities.
          </p>
          <p>
            Whether you want to engage in language exchange, build your global professional network, collaborate on digital projects, or participate in verified video discussions, Camverz provides a secure, seamless, and high-performance SaaS platform to connect instantly.
          </p>
        </section>

        {/* Core Values */}
        <section className={styles.valuesSection}>
          <h2 className={styles.sectionTitle}>Our Core Values</h2>
          <div className={styles.valuesGrid}>
            {values.map((v, i) => (
              <div key={i} className={styles.valueCard}>
                <div className={styles.iconCircle}>
                  <v.icon size={24} />
                </div>
                <h3>{v.title}</h3>
                <p>{v.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Platform Stats */}
        <section className={styles.statsCard}>
          <h2>Camverz by the Numbers</h2>
          <div className={styles.statsGrid}>
            <div className={styles.statItem}>
              <span className={styles.statNum}>50K+</span>
              <span className={styles.statLabel}>Active Daily Users</span>
            </div>
            <div className={styles.statItem}>
              <span className={styles.statNum}>1M+</span>
              <span className={styles.statLabel}>Connections Formed</span>
            </div>
            <div className={styles.statItem}>
              <span className={styles.statNum}>190+</span>
              <span className={styles.statLabel}>Countries Connected</span>
            </div>
            <div className={styles.statItem}>
              <span className={styles.statNum}>15s</span>
              <span className={styles.statLabel}>Average Match Time</span>
            </div>
          </div>
        </section>

      </div>
    </div>
  );
}
