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
            <span>🔥 18+ Live Random Video Chat & RealMeet Dating</span>
          </div>
          <h1 className="neon-text">About Camverz</h1>
          <p className={styles.subtitle}>
            Bringing back spontaneous, exciting 1-on-1 random video calls, in-person dating, and wild party hosting.
          </p>
        </section>

        {/* Story Section */}
        <section className={styles.storyCard}>
          <h2>Our Story</h2>
          <p>
            Camverz was built for adults who want real, unfiltered human connection. Tired of endless superficial swiping, fake profiles, and catfishing, we created a platform where you can jump into instant 1-on-1 live video chats with hot girls & boys worldwide.
          </p>
          <p>
            With RealMeet and Party Host, we take connection beyond the screen into real life: discover local 1-on-1 dates, coffee meetups, private house parties, and nightlife events in your city, plus explore your secret fantasies with open-minded adults discreetly and safely.
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
