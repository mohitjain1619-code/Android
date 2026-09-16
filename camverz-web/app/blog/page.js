'use client';
import { useState } from 'react';
import Link from 'next/link';
import { BookOpen, Calendar, Clock, ArrowRight, Sparkles } from 'lucide-react';
import styles from './page.module.css';

export const blogPosts = [
  {
    slug: 'best-free-alternative-to-holla-app',
    title: 'Best 100% Free Alternative to Holla App: Camverz Random Video Call & Community in 2026',
    description: 'Searching for the best 100% free alternative to Holla App? Compare Holla vs Camverz for screenshot protection, LGBTQ+ & straight matching, women safety, real meetups, and party hosts.',
    category: 'Comparison',
    date: 'September 17, 2026',
    readTime: '18 min read',
    image: '🔥',
    gradient: 'linear-gradient(135deg, rgba(255, 0, 110, 0.25) 0%, rgba(189, 0, 255, 0.25) 100%)',
    border: '#FF006E'
  },
  {
    slug: 'best-free-alternative-of-azar',
    title: 'Best 100% Free Alternative to Azar: Camverz Random Video Call & Community App in 2026',
    description: 'Discover why Camverz is the best 100% free alternative to Azar! Built with screenshot protection, women safety, LGBTQ+ inclusivity, real meetups, party hosts, and zero subscription paywalls.',
    category: 'Comparison',
    date: 'September 16, 2026',
    readTime: '18 min read',
    image: '⚡',
    gradient: 'linear-gradient(135deg, rgba(0, 229, 255, 0.25) 0%, rgba(189, 0, 255, 0.25) 100%)',
    border: '#00E5FF'
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
