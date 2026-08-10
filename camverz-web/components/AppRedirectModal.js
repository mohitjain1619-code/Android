'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '../lib/auth-context';
import { X, MessageSquare, Bell, Smartphone, Download, ArrowRight, Sparkles } from 'lucide-react';
import styles from './AppRedirectModal.module.css';

export default function AppRedirectModal() {
  const { showAppRedirect, setShowAppRedirect } = useAuth();
  const [activeTab, setActiveTab] = useState('messages'); // 'messages', 'notifications', 'posts'

  useEffect(() => {
    if (showAppRedirect) {
      setActiveTab(showAppRedirect);
    }
  }, [showAppRedirect]);

  if (!showAppRedirect) return null;

  const handleClose = () => {
    setShowAppRedirect(null);
  };

  const handleDeepLink = () => {
    // Attempt to open the custom URL scheme
    let deepLink = 'camverz://home';
    if (activeTab === 'messages') deepLink = 'camverz://inbox';
    else if (activeTab === 'notifications') deepLink = 'camverz://notifications';
    else if (activeTab === 'posts') deepLink = 'camverz://feed';
    
    window.location.href = deepLink;
    
    // Fallback alert after a delay in case app isn't installed
    setTimeout(() => {
      alert("If Camverz didn't open automatically, please make sure the mobile app is installed or scan the QR code to download the latest APK.");
    }, 1500);
  };

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className={styles.modalContent} onClick={e => e.stopPropagation()}>
        <button className={styles.closeBtn} onClick={handleClose} aria-label="Close">
          <X size={20} />
        </button>

        {/* Stunning Mock Background Layout representing the feature (fully blurred) */}
        <div className={styles.mockBackground}>
          <div className={`${styles.blurOverlay} ${styles.showBlur}`} />
          
          {activeTab === 'messages' ? (
            <div className={styles.mockChatContainer}>
              <div className={styles.mockChatHeader}>
                <div className={styles.mockAvatar} />
                <div className={styles.mockTitleText}>Sarah Connor</div>
              </div>
              <div className={styles.mockChatBody}>
                <div className={`${styles.mockBubble} ${styles.received}`}>Hey! Loved your post. Up for a call?</div>
                <div className={`${styles.mockBubble} ${styles.sent}`}>Absolutely! Switching to the app now.</div>
                <div className={`${styles.mockBubble} ${styles.received}`}>Awesome, see you there! 👋</div>
              </div>
            </div>
          ) : activeTab === 'notifications' ? (
            <div className={styles.mockNotificationContainer}>
              <div className={styles.mockNotificationItem}>
                <div className={styles.mockNotifIcon}>💖</div>
                <div className={styles.mockNotifText}><strong>Lisa</strong> liked your vanish post</div>
              </div>
              <div className={styles.mockNotificationItem}>
                <div className={styles.mockNotifIcon}>👤</div>
                <div className={styles.mockNotifText}><strong>David</strong> started following you</div>
              </div>
              <div className={styles.mockNotificationItem}>
                <div className={styles.mockNotifIcon}>⚡</div>
                <div className={styles.mockNotifText}><strong>New match</strong> found in Straight category</div>
              </div>
            </div>
          ) : (
            <div className={styles.mockPostContainer}>
              <div className={styles.mockPostCard}>
                <div className={styles.mockPostHeader}>
                  <div className={styles.mockAvatar} style={{ background: 'linear-gradient(135deg, var(--neon-cyan), var(--neon-blue))' }} />
                  <div>
                    <span className={styles.mockPostUser}>Alex Mercer</span>
                    <span className={styles.mockPostTag}>@alex</span>
                  </div>
                </div>
                <p className={styles.mockPostText}>Looking for someone to talk to tonight... dm me! ✨</p>
              </div>
              <div className={styles.mockPostCard}>
                <div className={styles.mockPostHeader}>
                  <div className={styles.mockAvatar} style={{ background: 'linear-gradient(135deg, var(--neon-pink), var(--neon-purple))' }} />
                  <div>
                    <span className={styles.mockPostUser}>Jessica</span>
                    <span className={styles.mockPostTag}>@jess</span>
                  </div>
                </div>
                <p className={styles.mockPostText}>Anyone up for a late night call? Straight category 💑</p>
              </div>
            </div>
          )}
        </div>

        {/* Floating Premium Card (Foreground) */}
        <div className={styles.redirectCard}>
          <div className={styles.badgeRow}>
            <span className={styles.appBadge}>
              <Sparkles size={12} className={styles.glowIcon} />
              Mobile Exclusive
            </span>
          </div>

          <div className={styles.tabHeader}>
            <button 
              className={`${styles.tabBtn} ${activeTab === 'messages' ? styles.activeTab : ''}`}
              onClick={() => setActiveTab('messages')}
            >
              <MessageSquare size={14} />
              <span>Messages</span>
            </button>
            <button 
              className={`${styles.tabBtn} ${activeTab === 'posts' ? styles.activeTab : ''}`}
              onClick={() => setActiveTab('posts')}
            >
              <Sparkles size={14} />
              <span>Posts & Fun</span>
            </button>
            <button 
              className={`${styles.tabBtn} ${activeTab === 'notifications' ? styles.activeTab : ''}`}
              onClick={() => setActiveTab('notifications')}
            >
              <Bell size={14} />
              <span>Notifications</span>
            </button>
          </div>

          <div className={styles.cardContent}>
            <h2>
              {activeTab === 'messages' ? 'Direct Messaging' : activeTab === 'notifications' ? 'Instant Notifications' : 'Share & Interact'}
            </h2>
            <p className={styles.redirectDescription}>
              {activeTab === 'messages' 
                ? 'Instantly connect with verified members for direct networking and messaging.'
                : activeTab === 'notifications'
                ? 'Get instant notifications for connections, comments, updates, and matches.'
                : 'Download the official mobile app to share thoughts, post updates, comment on community threads, and connect with peers worldwide.'
              }
            </p>

            {/* Action Buttons */}
            <div className={styles.actionsColumn}>
              <button className="btn-neon" onClick={handleDeepLink} style={{ width: '100%' }}>
                <Smartphone size={18} /> Open Camverz Mobile App <ArrowRight size={16} />
              </button>
              
              <div className={styles.divider}>
                <span>or download the app</span>
              </div>

              <div className={styles.downloadRow}>
                <a href="/downloads/camverz-latest.apk" className={styles.downloadBtn} download>
                  <Download size={16} />
                  <div>
                    <span className={styles.downloadSmall}>Download APK</span>
                    <span className={styles.downloadLarge}>Android App</span>
                  </div>
                </a>

                {/* SVG Mock QR Code */}
                <div className={styles.qrContainer} title="Scan QR Code to download">
                  <svg className={styles.qrSvg} viewBox="0 0 100 100">
                    {/* QR Code Outer Frame */}
                    <rect x="5" y="5" width="25" height="25" fill="none" stroke="var(--neon-cyan)" strokeWidth="4" rx="2" />
                    <rect x="10" y="10" width="15" height="15" fill="var(--neon-cyan)" rx="1" />
                    
                    <rect x="70" y="5" width="25" height="25" fill="none" stroke="var(--neon-cyan)" strokeWidth="4" rx="2" />
                    <rect x="75" y="10" width="15" height="15" fill="var(--neon-cyan)" rx="1" />

                    <rect x="5" y="70" width="25" height="25" fill="none" stroke="var(--neon-cyan)" strokeWidth="4" rx="2" />
                    <rect x="10" y="75" width="15" height="15" fill="var(--neon-cyan)" rx="1" />

                    {/* QR Code Dots */}
                    <circle cx="45" cy="15" r="3" fill="var(--neon-purple)" />
                    <circle cx="55" cy="20" r="2" fill="var(--neon-pink)" />
                    <circle cx="40" cy="30" r="3.5" fill="var(--neon-cyan)" />
                    <circle cx="50" cy="40" r="2.5" fill="var(--text-muted)" />
                    <circle cx="60" cy="35" r="3" fill="var(--neon-purple)" />
                    
                    <circle cx="45" cy="75" r="3" fill="var(--neon-cyan)" />
                    <circle cx="55" cy="85" r="2" fill="var(--neon-pink)" />
                    <circle cx="40" cy="60" r="3.5" fill="var(--neon-purple)" />
                    <circle cx="50" cy="70" r="2.5" fill="var(--text-muted)" />
                    
                    <circle cx="80" cy="50" r="3.5" fill="var(--neon-cyan)" />
                    <circle cx="85" cy="65" r="2.5" fill="var(--neon-pink)" />
                    <circle cx="70" cy="60" r="3" fill="var(--neon-purple)" />
                    
                    <circle cx="20" cy="45" r="2.5" fill="var(--neon-purple)" />
                    <circle cx="30" cy="50" r="3" fill="var(--neon-pink)" />
                    <circle cx="15" cy="55" r="3.5" fill="var(--neon-cyan)" />

                    {/* Glowing Thunder Logo in center */}
                    <g transform="translate(42, 42)">
                      <circle cx="8" cy="8" r="10" fill="#060612" stroke="var(--neon-cyan)" strokeWidth="1" />
                      <text x="8" y="12" fill="var(--neon-cyan)" fontSize="12" fontWeight="bold" textAnchor="middle">⚡</text>
                    </g>
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
