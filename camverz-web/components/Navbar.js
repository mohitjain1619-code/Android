'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { Menu, X, Video, User, FileText, BookOpen, Info, Mail, LogOut, Crown, MessageSquare, Bell, Zap } from 'lucide-react';
import styles from './Navbar.module.css';

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const pathname = usePathname();
  const { user, userData, signOut, setShowLogin, setShowAppRedirect } = useAuth();

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    const frame = requestAnimationFrame(() => setMenuOpen(false));
    return () => cancelAnimationFrame(frame);
  }, [pathname]);

  const links = [
    { href: '/', label: 'Home', icon: Video },
    { href: '/pricing', label: 'Pricing', icon: Crown },
    { href: '/posts', label: 'Posts', icon: FileText },
    { href: '/affiliate', label: 'Creator Program', icon: Crown },
    { href: '#messages', label: 'Messages', icon: MessageSquare, redirect: 'messages' },
    { href: '#notifications', label: 'Notifications', icon: Bell, redirect: 'notifications' },
    { href: '/blog', label: 'Blog', icon: BookOpen },
    { href: '/about', label: 'About', icon: Info },
    { href: '/contact', label: 'Contact', icon: Mail },
  ];

  const avatarSrc = userData?.avatar
    ? `/avatars/${userData.avatar}.png`
    : '/avatars/av1.png';

  return (
    <>
      <nav className={`${styles.nav} ${scrolled ? styles.scrolled : ''}`}>
        <div className={styles.inner}>
          <Link href="/" className={styles.logo}>
            <span className={styles.logoIcon} aria-hidden="true"><Zap size={18} strokeWidth={2.5} /></span>
            <span className={styles.logoText}>Camverz</span>
          </Link>

          <div className={styles.desktopLinks}>
            {links.map(l => {
              if (l.redirect) {
                return (
                  <button
                    key={l.label}
                    onClick={() => setShowAppRedirect(l.redirect)}
                    className={styles.navLink}
                  >
                    {l.label}
                  </button>
                );
              }
              return (
                <Link key={l.href} href={l.href} className={`${styles.navLink} ${pathname === l.href ? styles.active : ''}`}>
                  {l.label}
                </Link>
              );
            })}
          </div>

          <div className={styles.actions}>
            {user ? (
              <Link href="/profile" className={styles.profileBtn}>
                <img src={avatarSrc} alt="Profile" className={styles.navAvatar} />
                <span className="hide-mobile">{userData?.name?.split(' ')[0] || 'Profile'}</span>
              </Link>
            ) : (
              <button onClick={() => setShowLogin(true)} className="btn-neon" style={{ padding: '8px 20px', fontSize: '0.85rem' }}>
                Sign In
              </button>
            )}
            <button className={styles.hamburger} onClick={() => setMenuOpen(!menuOpen)} aria-label="Menu">
              {menuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </nav>

      {/* Mobile Menu */}
      <div className={`${styles.mobileMenu} ${menuOpen ? styles.mobileMenuOpen : ''}`}>
        <div className={styles.mobileMenuInner}>
          {links.map((l, i) => {
            if (l.redirect) {
              return (
                <button
                  key={l.label}
                  className={styles.mobileLink}
                  style={{ animationDelay: `${i * 0.05}s` }}
                  onClick={() => {
                    setMenuOpen(false);
                    setShowAppRedirect(l.redirect);
                  }}
                >
                  <l.icon size={20} />
                  <span>{l.label}</span>
                </button>
              );
            }
            return (
              <Link
                key={l.href}
                href={l.href}
                className={`${styles.mobileLink} ${pathname === l.href ? styles.mobileLinkActive : ''}`}
                style={{ animationDelay: `${i * 0.05}s` }}
                onClick={() => setMenuOpen(false)}
              >
                <l.icon size={20} />
                <span>{l.label}</span>
              </Link>
            );
          })}
          {user && (
            <>
              <Link href="/profile" className={styles.mobileLink} onClick={() => setMenuOpen(false)}>
                <User size={20} />
                <span>Profile</span>
              </Link>
              <button className={styles.mobileLink} onClick={() => { signOut(); setMenuOpen(false); }}>
                <LogOut size={20} />
                <span>Sign Out</span>
              </button>
            </>
          )}
          {!user && (
            <button className={styles.mobileLink} onClick={() => { setMenuOpen(false); setShowLogin(true); }}>
              <Crown size={20} />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </div>
    </>
  );
}