import Link from 'next/link';
import styles from './Footer.module.css';
import { Video, Shield, Heart, Mail } from 'lucide-react';

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div className={styles.grid}>
          <div className={styles.brand}>
            <div className={styles.logo}>
              <span>⚡</span>
              <span className={styles.logoText}>Camverz</span>
            </div>
            {/* <p className={styles.tagline}>Connect with real people through random video calls. Safe, verified, and fun.</p> */}
            <p className={styles.tagline}>Virtual networking & SaaS community platform. Connect with verified members worldwide.</p>
          </div>

          <div className={styles.linkGroup}>
            <h4>Platform</h4>
            <Link href="/">Home</Link>
            <Link href="/pricing">Pricing</Link>
            <Link href="/posts">Posts</Link>
            <Link href="/affiliate">Creator Program</Link>
            <Link href="/blog">Blog</Link>
            <Link href="/about">About</Link>
            <Link href="/contact">Contact</Link>
          </div>

          <div className={styles.linkGroup}>
            <h4>Legal</h4>
            <Link href="/legal/child-safety">Child Safety Policy</Link>
            <Link href="/legal/privacy-policy">Privacy Policy</Link>
            <Link href="/legal/terms-and-conditions">Terms & Conditions</Link>
            <Link href="/legal/refund-policy">Refund Policy</Link>
            <Link href="/legal/cookie-policy">Cookie Policy</Link>
            <Link href="/legal/community-guidelines">Community Guidelines</Link>
          </div>

          <div className={styles.linkGroup}>
            <h4>Support</h4>
            <Link href="/contact">Help Center</Link>
            <a href="mailto:support@camverz.com">support@camverz.com</a>
          </div>
        </div>

        <div className={styles.bottom}>
          <div>
            <p>© {new Date().getFullYear()} Camverz. All rights reserved.</p>
            <p style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px', opacity: 0.8 }}>Owned & operated by: Mohit Jain</p>
          </div>
          <div className={styles.badges}>
            <span className={styles.badge}><Shield size={14} /> Verified Users</span>
            <span className={styles.badge}><Heart size={14} /> Safe Space</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
