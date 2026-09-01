'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { 
  Menu, X, Video, User, FileText, BookOpen, Info, Mail, LogOut, Crown, 
  MessageSquare, Bell, Zap, Sparkles, Send, Check
} from 'lucide-react';
import { 
  getChats, 
  getMessages, 
  sendMessage, 
  markChatRead, 
  getNotifications, 
  getRealMeetRequests, 
  getCommunityNotifications, 
  markNotificationRead 
} from '../lib/api';
import styles from './Navbar.module.css';

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  const { user, userData, signOut, setShowLogin, setShowAppRedirect } = useAuth();

  // Sliding Drawer States
  const [showChatDrawer, setShowChatDrawer] = useState(false);
  const [chats, setChats] = useState([]);
  const [activeChat, setActiveChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [chatText, setChatText] = useState('');
  const [msgsLoading, setMsgsLoading] = useState(false);

  const [showNotificationsDrawer, setShowNotificationsDrawer] = useState(false);
  const [notifications, setNotifications] = useState([]);

  const isRealMeet = pathname === '/realmeet';

  const links = isRealMeet ? [
    { href: '/', label: '↩ Back to Video Calling', icon: Video },
    { href: '/realmeet', label: 'Real Meet Feed', icon: Sparkles },
    { href: '#messages', label: 'RM Messages', icon: MessageSquare, isDrawer: 'messages' },
    { href: '#notifications', label: 'RM Alerts', icon: Bell, isDrawer: 'notifications' },
  ] : [
    { href: '/', label: 'Home', icon: Video },
    { href: '/realmeet', label: 'Real Meet', icon: Sparkles },
    { href: '/posts', label: 'Posts', icon: FileText },
    { href: '#messages', label: 'Messages', icon: MessageSquare, isDrawer: 'messages' },
    { href: '#notifications', label: 'Notifications', icon: Bell, isDrawer: 'notifications' },
    { href: '/blog', label: 'Blog', icon: BookOpen },
    { href: '/about', label: 'About', icon: Info },
    { href: '/contact', label: 'Contact', icon: Mail },
  ];

  const handleLinkClick = (l) => {
    if (l.isDrawer) {
      if (l.isDrawer === 'messages') {
        setShowChatDrawer(true);
        setShowNotificationsDrawer(false);
      } else {
        setShowNotificationsDrawer(true);
        setShowChatDrawer(false);
      }
      setMenuOpen(false);
    } else {
      router.push(l.href);
    }
  };

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    const frame = requestAnimationFrame(() => setMenuOpen(false));
    return () => cancelAnimationFrame(frame);
  }, [pathname]);

  // Fetch chats and notifications periodically
  useEffect(() => {
    if (!user) return;

    const loadInboxData = async () => {
      try {
        // Fetch chats
        const chatsRes = await getChats();
        if (chatsRes.ok) {
          // If in Real Meet, we want to filter chats to only include users from Community Requests
          if (isRealMeet) {
            const reqRes = await getRealMeetRequests();
            if (reqRes.ok) {
              const matchedUserIds = (reqRes.requests || [])
                .filter(r => r.status?.toUpperCase() === 'ACCEPTED')
                .map(r => r.applicantUserId === user.uid ? r.posterUserId : r.applicantUserId);
              setChats((chatsRes.chats || []).filter(c => c.otherUser && matchedUserIds.includes(c.otherUser.id)));
            } else {
              setChats([]);
            }
          } else {
            // General Video Calling chats (exclude Real Meet accepted connections)
            const reqRes = await getRealMeetRequests();
            if (reqRes.ok) {
              const matchedUserIds = (reqRes.requests || [])
                .filter(r => r.status?.toUpperCase() === 'ACCEPTED')
                .map(r => r.applicantUserId === user.uid ? r.posterUserId : r.applicantUserId);
              setChats((chatsRes.chats || []).filter(c => c.otherUser && !matchedUserIds.includes(c.otherUser.id)));
            } else {
              setChats(chatsRes.chats || []);
            }
          }
        }

        // Fetch notifications
        if (isRealMeet) {
          const notRes = await getCommunityNotifications();
          if (notRes.ok) {
            setNotifications(notRes.notifications || []);
          }
        } else {
          const notRes = await getNotifications(50, 0);
          if (notRes.ok) {
            // Filter out Real Meet notification types
            const filtered = (notRes.notifications || []).filter(n => 
              n.type !== 'realmeet_request' && 
              n.type !== 'realmeet_accepted' && 
              n.type !== 'party_announcement'
            );
            setNotifications(filtered);
          }
        }
      } catch (err) {
        console.error("Error fetching inbox data:", err);
      }
    };

    loadInboxData();
    const interval = setInterval(loadInboxData, 5000);
    return () => clearInterval(interval);
  }, [user, isRealMeet]);

  // Load messages of active chat
  const loadMessages = async (chatId) => {
    setMsgsLoading(true);
    try {
      const res = await getMessages(chatId);
      if (res.ok) {
        setMessages(res.messages || []);
      }
    } catch (e) {
      console.error("Error loading messages:", e);
    } finally {
      setMsgsLoading(false);
    }
  };

  useEffect(() => {
    if (!activeChat) return;
    loadMessages(activeChat.id);
    markChatRead(activeChat.id).catch(() => {});
    
    const msgInterval = setInterval(() => {
      getMessages(activeChat.id).then(res => {
        if (res.ok) {
          setMessages(res.messages || []);
        }
      });
    }, 3000);

    return () => clearInterval(msgInterval);
  }, [activeChat]);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!chatText.trim() || !activeChat || !activeChat.otherUser) return;
    const textToSend = chatText.trim();
    setChatText('');
    try {
      const res = await sendMessage(activeChat.otherUser.id, textToSend);
      if (res.ok) {
        setMessages(prev => [...prev, {
          id: Math.random().toString(),
          senderId: user.uid,
          text: textToSend,
          createdAt: new Date().toISOString(),
          isMe: true
        }]);
      }
    } catch (err) {
      console.error("Failed to send message:", err);
    }
  };

  const handleMarkNotificationRead = async (notifId) => {
    try {
      await markNotificationRead(notifId);
      setNotifications(prev => prev.map(n => n.id === notifId ? { ...n, read: true } : n));
    } catch (e) {}
  };

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
              if (l.isDrawer) {
                return (
                  <button
                    key={l.label}
                    onClick={() => handleLinkClick(l)}
                    className={`${styles.navLink} ${pathname === l.href ? styles.active : ''}`}
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
            <Link href={isRealMeet ? "/pricing?tab=realmeet" : "/pricing?category=video-call"} className={styles.pricingPill}>
              💎 Pricing
            </Link>

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
            if (l.isDrawer) {
              return (
                <button
                  key={l.label}
                  className={styles.mobileLink}
                  style={{ animationDelay: `${i * 0.05}s` }}
                  onClick={() => handleLinkClick(l)}
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

      {/* Messages Drawer */}
      {showChatDrawer && (
        <div className={styles.drawerOverlay} onClick={() => setShowChatDrawer(false)} />
      )}
      <div className={`${styles.drawer} ${showChatDrawer ? styles.drawerOpen : ''}`}>
        <div className={styles.drawerHeader}>
          <h3>{isRealMeet ? '📩 Real Meet Chats' : '📩 Video Call Inbox'}</h3>
          <button className={styles.drawerClose} onClick={() => setShowChatDrawer(false)}><X size={20} /></button>
        </div>
        
        {activeChat ? (
          <div className={styles.chatRoom}>
            <div className={styles.chatRoomHeader}>
              <button className={styles.backBtn} onClick={() => setActiveChat(null)}>
                ← Back
              </button>
              <span className={styles.roomTitle}>{activeChat.otherUser?.name}</span>
            </div>
            
            <div className={styles.messagesList}>
              {messages.map(m => (
                <div key={m.id} className={`${styles.messageRow} ${m.senderId === user.uid ? styles.myMessage : styles.theirMessage}`}>
                  <div className={styles.messageBubble}>{m.text}</div>
                </div>
              ))}
            </div>
            
            <form onSubmit={handleSendMessage} className={styles.chatInputForm}>
              <input 
                type="text" 
                className={styles.chatInput} 
                placeholder="Type a message..." 
                value={chatText} 
                onChange={(e) => setChatText(e.target.value)} 
              />
              <button type="submit" className={styles.sendBtn}>
                <Send size={18} />
              </button>
            </form>
          </div>
        ) : (
          <div className={styles.drawerContent}>
            {chats.length === 0 ? (
              <p className={styles.emptyText}>No messages yet.</p>
            ) : (
              chats.map(c => (
                <button key={c.id} className={styles.chatItem} onClick={() => setActiveChat(c)}>
                  <img src={c.otherUser?.avatar ? `/avatars/${c.otherUser.avatar}.png` : '/avatars/av1.png'} alt="avatar" className={styles.avatar} />
                  <div className={styles.chatMeta}>
                    <div className={styles.chatName}>
                      {c.otherUser?.name}
                      {c.otherUser?.gender?.startsWith('f') && ' ♀️'}
                      {c.otherUser?.verified && <span style={{ marginLeft: '4px', color: '#00D9FF' }}>✓</span>}
                    </div>
                    <div className={styles.lastMsg}>{c.lastMessage || 'Click to chat'}</div>
                  </div>
                  {c.unreadCount > 0 && <span className={styles.unreadBadge}>{c.unreadCount}</span>}
                </button>
              ))
            )}
          </div>
        )}
      </div>

      {/* Notifications Drawer */}
      {showNotificationsDrawer && (
        <div className={styles.drawerOverlay} onClick={() => setShowNotificationsDrawer(false)} />
      )}
      <div className={`${styles.drawer} ${showNotificationsDrawer ? styles.drawerOpen : ''}`}>
        <div className={styles.drawerHeader}>
          <h3>{isRealMeet ? '🔔 Real Meet Alerts' : '🔔 Notifications'}</h3>
          <button className={styles.drawerClose} onClick={() => setShowNotificationsDrawer(false)}><X size={20} /></button>
        </div>
        <div className={styles.drawerContent}>
          {notifications.length === 0 ? (
            <p className={styles.emptyText}>No notifications yet.</p>
          ) : (
            notifications.map(n => {
              const uName = n.triggeringUser?.name || 'Someone';
              const uAvatar = n.triggeringUser?.avatar ? `/avatars/${n.triggeringUser.avatar}.png` : '/avatars/av1.png';
              
              let text = '';
              if (n.type === 'realmeet_request') {
                text = `${uName} sent you a meet request for "${n.postTitle}"`;
              } else if (n.type === 'realmeet_accepted') {
                text = `${uName} accepted your meet request for "${n.postTitle}"`;
              } else if (n.type === 'party_announcement') {
                text = `Announcement in party: "${n.postTitle}"`;
              } else if (n.type === 'friend_request') {
                text = `${uName} sent you a friend request`;
              } else if (n.type === 'like') {
                text = `${uName} liked your post`;
              } else if (n.type === 'comment') {
                text = `${uName} commented: "${n.postText || ''}"`;
              } else {
                text = `${uName} triggered an alert`;
              }

              return (
                <div 
                  key={n.id} 
                  className={`${styles.notifItem} ${!n.read ? styles.notifItemUnread : ''}`}
                  onClick={() => handleMarkNotificationRead(n.id)}
                >
                  <img src={uAvatar} alt="avatar" className={styles.notifAvatar} />
                  <div className={styles.notifBody}>
                    <div className={styles.notifText}>{text}</div>
                    <div className={styles.notifTime}>{new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                  </div>
                  {!n.read && <div className={styles.unreadDot} />}
                </div>
              );
            })
          )}
        </div>
      </div>
    </>
  );
}