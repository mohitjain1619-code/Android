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
          <p>
            Camverz was created to empower individuals, creators, and diverse social communities—including LGBTQ+ networks—to connect authentically through live video chat, story sharing, and real-time social communication.
          </p>
          <p>
            Whether you want to discover new friends, join inclusive social circles, practice languages, or share everyday moments on our social feed, Camverz provides a safe, seamless, and high-performance social networking platform.
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
