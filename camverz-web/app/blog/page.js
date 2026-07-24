'use client';
import { useState } from 'react';
import Link from 'next/link';
import { BookOpen, Calendar, Clock, ArrowRight, Sparkles } from 'lucide-react';
import styles from './page.module.css';

export const blogPosts = [
  {
    slug: 'safe-video-calling-tips',
    title: '5 Tips for Safe Video Calling with Strangers',
    description: 'Video calling online is an exciting way to meet new people, but safety should always be your top priority. Here are the 5 rules to stay safe.',
    category: 'Safety',
    date: 'June 20, 2026',
    readTime: '4 min read',
    image: '🔒',
    gradient: 'linear-gradient(135deg, rgba(0, 229, 255, 0.15) 0%, rgba(189, 0, 255, 0.15) 100%)',
    border: '#00E5FF'
  },
  {
    slug: 'video-chat-online-dating',
    title: 'How Random Video Chat is Changing Online Dating',
    description: 'Text profiles are out, live video is in. Discover how real-time, face-to-face random matching is changing how single people find romance.',
    category: 'Dating',
    date: 'June 18, 2026',
    readTime: '6 min read',
    image: '💑',
    gradient: 'linear-gradient(135deg, rgba(255, 0, 110, 0.15) 0%, rgba(189, 0, 255, 0.15) 100%)',
    border: '#FF006E'
  },
  {
    slug: 'genuine-connections-digital-age',
    title: 'Building Genuine Connections in the Digital Age',
    description: 'In a world dominated by superficial swiping, how do you hold a conversation that matters? Learn the art of deep digital dialogue.',
    category: 'Relationships',
    date: 'June 15, 2026',
    readTime: '5 min read',
    image: '✨',
    gradient: 'linear-gradient(135deg, rgba(0, 230, 86, 0.15) 0%, rgba(0, 229, 255, 0.15) 100%)',
    border: '#00E676'
  },
  {
    slug: 'gender-verification-importance',
    title: 'Understanding Gender Verification: Why It Matters',
    description: 'Safety, authenticity, and respect form the pillar of modern random video calling. Read about how gender verification keeps communities safe.',
    category: 'Community',
    date: 'June 10, 2026',
    readTime: '3 min read',
    image: '🛡️',
    gradient: 'linear-gradient(135deg, rgba(189, 0, 255, 0.15) 0%, rgba(255, 109, 0, 0.15) 100%)',
    border: '#BD00FF'
  },
  {
    slug: 'long-distance-relationships-tips',
    title: 'Long-Distance Relationships: Making Them Work',
    description: 'Are you in a long-distance relationship or met someone from another country on Camverz? Here are key strategies to maintain the spark.',
    category: 'Relationships',
    date: 'June 05, 2026',
    readTime: '7 min read',
    image: '✈️',
    gradient: 'linear-gradient(135deg, rgba(255, 234, 0, 0.15) 0%, rgba(255, 0, 110, 0.15) 100%)',
    border: '#FFEA00'
  },
  {
    slug: 'online-safety-privacy-guide',
    title: 'Online Safety Guide: Protecting Your Privacy',
    description: 'A comprehensive guide to protecting your personal data, reports systems, blocking bad actors, and managing your digital footprint.',
    category: 'Safety',
    date: 'May 28, 2026',
    readTime: '8 min read',
    image: '🛡️',
    gradient: 'linear-gradient(135deg, rgba(255, 109, 0, 0.15) 0%, rgba(0, 229, 255, 0.15) 100%)',
    border: '#FF6D00'
  }
];

export default function BlogPage() {
  const [selectedCategory, setSelectedCategory] = useState('All');
  const categories = ['All', 'Safety', 'Dating', 'Relationships', 'Community'];

  const filteredPosts = selectedCategory === 'All'
    ? blogPosts
    : blogPosts.filter(post => post.category === selectedCategory);

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Header */}
        <header className={styles.header}>
          <div className={styles.headerBadge}>
            <Sparkles size={14} />
            <span>Camverz Articles & Guides</span>
          </div>
          <h1 className="neon-text">The Camverz Blog</h1>
          <p>Read about random video chats, online safety, relationship building, and tips on finding matches.</p>
        </header>

        {/* Category filters */}
        <div className={styles.categoriesRow}>
          {categories.map(cat => (
            <button
              key={cat}
              className={`${styles.categoryTab} ${selectedCategory === cat ? styles.active : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Blog Grid */}
        <div className={styles.grid}>
          {filteredPosts.map(post => (
            <article 
              key={post.slug} 
              className={styles.blogCard}
              style={{ '--card-border': post.border }}
            >
              <div className={styles.cardImageArea} style={{ background: post.gradient }}>
                <span className={styles.cardEmoji}>{post.image}</span>
                <span className={styles.cardCategory}>{post.category}</span>
              </div>
              <div className={styles.cardContent}>
                <div className={styles.metaRow}>
                  <span className={styles.metaItem}>
                    <Calendar size={12} /> {post.date}
                  </span>
                  <span className={styles.metaItem}>
                    <Clock size={12} /> {post.readTime}
                  </span>
                </div>
                <h3>{post.title}</h3>
                <p>{post.description}</p>
                
                <Link href={`/blog/${post.slug}`} className={styles.readMoreLink}>
                  <span>Read Full Article</span>
                  <ArrowRight size={16} />
                </Link>
              </div>
            </article>
          ))}
        </div>

      </div>
    </div>
  );
}
