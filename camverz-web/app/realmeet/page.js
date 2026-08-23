'use client';
import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useAuth } from '../../lib/auth-context';
import { 
  getRealMeetFeed, 
  createRealMeetPost, 
  deleteRealMeetPost, 
  getRealMeetRequests, 
  sendRealMeetRequest, 
  updateRealMeetRequestStatus,
  updateMe,
  getSavedParties,
  saveParty,
  unsaveParty,
  getPartyMembers,
  setPartyVisibility,
  postAnnouncement,
  getAnnouncements,
  getCommunityNotifications
} from '../../lib/api';
import { 
  Sparkles, 
  MapPin, 
  Clock, 
  Send, 
  Users, 
  Heart, 
  Trash2, 
  PlusCircle, 
  User, 
  AlertCircle, 
  Check, 
  X as CloseIcon, 
  ShieldAlert,
  Bookmark,
  Bell,
  Megaphone,
  UserPlus
} from 'lucide-react';
import styles from './page.module.css';

// Instagram-style blue verified checkmark badge component
function VerifiedBadge() {
  return (
    <svg viewBox="0 0 24 24" fill="#00D9FF" width="14" height="14" style={{ display: 'inline-block', verticalAlign: 'middle', marginLeft: '4px' }}>
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10,-4.48 10,-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
    </svg>
  );
}

function RealMeetContent() {
  const { user, userData, loading: authLoading, setShowLogin, setShowAppRedirect } = useAuth();
  const searchParams = useSearchParams();
  const action = searchParams.get('action');
  
  // Selected Tab: 'meet' | 'party' | 'fantasy'
  const [activeTab, setActiveTab] = useState('meet');
  const [loading, setLoading] = useState(true);
  
  // Feeds data
  const [realMeetPosts, setRealMeetPosts] = useState([]);
  const [partyPosts, setPartyPosts] = useState([]);
  const [fantasyPosts, setFantasyPosts] = useState([]);
  const [requests, setRequests] = useState([]);
  
  const [isAndroid, setIsAndroid] = useState(false);

  // New persistent states
  const [savedParties, setSavedParties] = useState([]);
  const [savedPartyIds, setSavedPartyIds] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [showNotificationsDrawer, setShowNotificationsDrawer] = useState(false);
  const [notifTab, setNotifTab] = useState('meet');
  const [partyMembers, setPartyMembers] = useState({});
  const [partyAnnouncements, setPartyAnnouncements] = useState({});
  const [announcementTexts, setAnnouncementTexts] = useState({});

  // Preferences Modal State
  const [showPreferencePopup, setShowPreferencePopup] = useState(false);
  const [selectedPreference, setSelectedPreference] = useState('');

  // Check if preferences modal should trigger (if logged in, preference not set in DB yet, and not Android)
  useEffect(() => {
    if (userData && !userData.sexPreference && !isAndroid) {
      if (localStorage.getItem('sex_preference_selected_v2') !== 'true') {
        setShowPreferencePopup(true);
      }
    }
  }, [userData, isAndroid]);
  
  // Composer Form Inputs
  const [meetPurpose, setMeetPurpose] = useState('');
  const [meetLocation, setMeetLocation] = useState('');
  const [meetTime, setMeetTime] = useState('');
  const [meetDescription, setMeetDescription] = useState('');
  
  const [partyTitle, setPartyTitle] = useState('');
  const [partyVenue, setPartyVenue] = useState('');
  const [partyTime, setPartyTime] = useState('');
  const [partyCapacity, setPartyCapacity] = useState('20');
  const [partyTargetGender, setPartyTargetGender] = useState('Everyone');
  
  const [fantasyStatus, setFantasyStatus] = useState('Single');
  const [fantasyInterests, setFantasyInterests] = useState('');
  const [fantasyDescription, setFantasyDescription] = useState('');
  
  const [posting, setPosting] = useState(false);

  // 1. Detect Device
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const ua = navigator.userAgent || navigator.vendor || window.opera;
      setIsAndroid(/android/i.test(ua) || /windows/i.test(ua));
    }
  }, []);

  // 1b. Handle search query action parameters (for messages/notifications feature parity)
  useEffect(() => {
    if (!action) return;
    if (action === 'notifications') {
      setShowNotificationsDrawer(true);
    } else if (action === 'messages') {
      setTimeout(() => {
        const el = document.getElementById('realmeet-inbox');
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 600);
    }
  }, [action]);

  // 2. Fetch Feed and Requests
  const fetchFeedData = async () => {
    if (!user) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      // Feed
      const feedRes = await getRealMeetFeed();
      if (feedRes.ok) {
        setRealMeetPosts(feedRes.realMeetPosts || []);
        setPartyPosts(feedRes.partyPosts || []);
        setFantasyPosts(feedRes.fantasyPosts || []);

        // Load members list & announcements for party events
        const pPosts = feedRes.partyPosts || [];
        for (let post of pPosts) {
          try {
            const memRes = await getPartyMembers(post.id);
            if (memRes.ok) {
              setPartyMembers(prev => ({ ...prev, [post.id]: memRes.members }));
            }
          } catch (e) {}
          try {
            const annRes = await getAnnouncements(post.id);
            if (annRes.ok) {
              setPartyAnnouncements(prev => ({ ...prev, [post.id]: annRes.announcements }));
            }
          } catch (e) {}
        }
      }
      
      // Requests
      const reqRes = await getRealMeetRequests();
      if (reqRes.ok) {
        setRequests(reqRes.requests || []);
      }

      // Saved Parties
      const savedRes = await getSavedParties();
      if (savedRes.ok) {
        setSavedParties(savedRes.savedParties || []);
        setSavedPartyIds((savedRes.savedParties || []).map(p => p.id));
      }

      // Notifications
      const notRes = await getCommunityNotifications();
      if (notRes.ok) {
        setNotifications(notRes.notifications || []);
      }
    } catch (err) {
      console.error('Error loading Real Meet data:', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleSaveParty = async (postId) => {
    if (!user) return;
    try {
      const isSaved = savedPartyIds.includes(postId);
      if (isSaved) {
        setSavedPartyIds(prev => prev.filter(id => id !== postId));
        await unsaveParty(postId);
      } else {
        setSavedPartyIds(prev => [...prev, postId]);
        await saveParty(postId);
      }
      const savedRes = await getSavedParties();
      if (savedRes.ok) {
        setSavedParties(savedRes.savedParties || []);
      }
    } catch (err) {
      console.error('Failed to toggle party save state:', err);
    }
  };

  const handleUpdateVisibility = async (postId, visibility) => {
    try {
      const res = await setPartyVisibility(postId, visibility);
      if (res.ok) {
        alert(`Guest list visibility changed to ${visibility}`);
        await fetchFeedData();
      }
    } catch (err) {
      console.error('Failed to update visibility:', err);
    }
  };

  const handlePostAnnouncement = async (postId) => {
    const text = announcementTexts[postId];
    if (!text) return;
    try {
      const res = await postAnnouncement(postId, text);
      if (res.ok) {
        alert('Announcement posted successfully!');
        setAnnouncementTexts(prev => ({ ...prev, [postId]: '' }));
        await fetchFeedData();
      }
    } catch (err) {
      console.error('Failed to post announcement:', err);
    }
  };

  useEffect(() => {
    if (authLoading) return;
    fetchFeedData();
  }, [user, authLoading]);

  // 3. Handle Lock Checks
  const checkLockAndExecute = (actionKey, callback) => {
    if (isAndroid) {
      setShowAppRedirect(actionKey);
    } else {
      callback();
    }
  };

  // 4. Create Post Handler
  const handleCreatePost = async (e) => {
    e.preventDefault();
    if (!user) return;
    
    checkLockAndExecute('posts', async () => {
      setPosting(true);
      try {
        let postPayload = {};
        let type = '';

        if (activeTab === 'meet') {
          if (!meetPurpose || !meetLocation || !meetTime || !meetDescription) {
            alert('Please fill out all meet fields.');
            setPosting(false);
            return;
          }
          type = 'REAL_MEET';
          postPayload = {
            id: Math.random().toString(36).substring(2) + Date.now(),
            userId: user.uid,
            userName: userData?.name || 'User',
            userAvatar: userData?.avatar || 'av1',
            photoUrl: '',
            age: userData?.dob ? calculateAge(userData.dob) : 22,
            city: userData?.city || 'Nearby',
            purpose: meetPurpose,
            location: meetLocation,
            time: meetTime,
            description: meetDescription,
            gender: userData?.gender || 'male',
            verified: userData?.verified || false,
            premium: userData?.verified || false, // verified are premium
            sexPreference: userData?.sexPreference || 'Straight',
            createdAt: Date.now()
          };
        } else if (activeTab === 'party') {
          if (!partyTitle || !partyVenue || !partyTime) {
            alert('Please fill out all party fields.');
            setPosting(false);
            return;
          }
          type = 'PARTY';
          postPayload = {
            id: Math.random().toString(36).substring(2) + Date.now(),
            hostUserId: user.uid,
            hostName: userData?.name || 'User',
            hostAvatar: userData?.avatar || 'av1',
            hostPhotoUrl: '',
            hostAge: userData?.dob ? calculateAge(userData.dob) : 22,
            venue: partyVenue,
            purpose: partyTitle,
            capacity: parseInt(partyCapacity),
            targetGender: partyTargetGender,
            partyTime: partyTime,
            gender: userData?.gender || 'male',
            verified: userData?.verified || false,
            premium: userData?.verified || false,
            createdAt: Date.now()
          };
        } else if (activeTab === 'fantasy') {
          if (!fantasyDescription) {
            alert('Please fill out your fantasy description.');
            setPosting(false);
            return;
          }
          type = 'FANTASY';
          postPayload = {
            id: Math.random().toString(36).substring(2) + Date.now(),
            userId: user.uid,
            userName: userData?.name || 'User',
            userAvatar: userData?.avatar || 'av1',
            photoUrl: '',
            age: userData?.dob ? calculateAge(userData.dob) : 22,
            relationshipStatus: fantasyStatus,
            description: fantasyDescription,
            interests: fantasyInterests || 'General',
            gender: userData?.gender || 'male',
            verified: userData?.verified || false,
            premium: userData?.verified || false,
            createdAt: Date.now()
          };
        }

        const res = await createRealMeetPost(type, postPayload);
        if (res.ok) {
          // Clear inputs
          setMeetPurpose('');
          setMeetLocation('');
          setMeetTime('');
          setMeetDescription('');
          setPartyTitle('');
          setPartyVenue('');
          setPartyTime('');
          setFantasyInterests('');
          setFantasyDescription('');
          
          // Refresh Feed
          await fetchFeedData();
        }
      } catch (err) {
        console.error('Failed to publish post:', err);
      } finally {
        setPosting(false);
      }
    });
  };

  // 5. Delete Post Handler
  const handleDeletePost = async (id) => {
    if (!confirm('Are you sure you want to delete this post?')) return;
    try {
      const res = await deleteRealMeetPost(id);
      if (res.ok) {
        await fetchFeedData();
      }
    } catch (err) {
      console.error('Failed to delete post:', err);
    }
  };

  const handleSendRequest = async (post) => {
    checkLockAndExecute('messages', async () => {
      // Prevent multiple requests
      const targetUserId = post.userId || post.hostUserId;
      const alreadyRequested = requests.some(r => r.fromUserId === user.uid && r.toUserId === targetUserId);
      if (alreadyRequested) {
        alert('You have already sent a request to this user.');
        return;
      }

      // Enforce Verified Female Only constraint for party posts
      if (post.targetGender === 'Female Only') {
        const isFemale = userData?.gender === 'female';
        const isVerified = userData?.verified === true;
        if (!isFemale || !isVerified) {
          alert('🔒 Only verified females can join this party event.');
          return;
        }
      }

      try {
        const reqPayload = {
          id: Math.random().toString(36).substring(2) + Date.now(),
          fromUserId: user.uid,
          fromUserName: userData?.name || 'User',
          fromUserAvatar: userData?.avatar || 'av1',
          fromUserGender: userData?.gender || 'male',
          fromUserVerified: userData?.verified || false,
          toUserId: targetUserId,
          postTitle: post.purpose || 'Real Meet Connect',
          postId: post.id,
          status: 'requested',
          createdAt: Date.now()
        };

        const res = await sendRealMeetRequest(reqPayload);
        if (res.ok) {
          alert('Meet request sent successfully!');
          await fetchFeedData();
        }
      } catch (err) {
        console.error('Failed to send request:', err);
      }
    });
  };

  // 7. Accept/Reject Request
  const handleUpdateStatus = async (requestId, status) => {
    checkLockAndExecute('messages', async () => {
      try {
        const res = await updateRealMeetRequestStatus(requestId, status);
        if (res.ok) {
          await fetchFeedData();
        }
      } catch (err) {
        console.error('Failed to update request status:', err);
      }
    });
  };

  const calculateAge = (dobString) => {
    if (!dobString) return 22;
    const parts = dobString.split('/');
    if (parts.length === 3) {
      const day = parseInt(parts[0]);
      const month = parseInt(parts[1]) - 1;
      const year = parseInt(parts[2]);
      const birth = new Date(year, month, day);
      const today = new Date();
      let age = today.getFullYear() - birth.getFullYear();
      const m = today.getMonth() - birth.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
        age--;
      }
      return age;
    }
    return 22;
  };

  const activeTagline = () => {
    if (activeTab === 'meet') return '✨ See who wanted to meet you in real life';
    if (activeTab === 'party') return '🎉 Host the real life party or host the real event in your city';
    return '💬 Share your fantasy and find matching vibes';
  };

  return (
    <>
      <div className={styles.container}>
        <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Header */}
        <header className={styles.header} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h1 className="neon-text">Real Meet Hub</h1>
            <p>Facilitating authentic real-world connections, social house parties, and shared fantasies.</p>
          </div>
          {user && (
            <button 
              className={styles.bellBtn} 
              onClick={() => setShowNotificationsDrawer(true)}
              style={{ position: 'relative', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)', padding: '10px', borderRadius: '50%', cursor: 'pointer', transition: 'all 0.2s' }}
            >
              <Bell size={20} color="#00FFFF" />
              {notifications.length > 0 && (
                <span className={styles.notifBadge} style={{ position: 'absolute', top: '-5px', right: '-5px', background: '#EF4444', color: '#FFFFFF', borderRadius: '50%', padding: '2px 6px', fontSize: '10px', fontWeight: 'bold' }}>
                  {notifications.length}
                </span>
              )}
            </button>
          )}
        </header>

        {/* Tab Selection */}
        <div className={styles.tabsContainer}>
          <button 
            onClick={() => setActiveTab('meet')}
            className={`${styles.tabBtn} ${activeTab === 'meet' ? styles.activeTabRealMeet : ''}`}
          >
            ⚡ Real Meet
          </button>
          <button 
            onClick={() => setActiveTab('party')}
            className={`${styles.tabBtn} ${activeTab === 'party' ? styles.activeTabParty : ''}`}
          >
            🥳 Real Party
          </button>
          <button 
            onClick={() => setActiveTab('fantasy')}
            className={`${styles.tabBtn} ${activeTab === 'fantasy' ? styles.activeTabFantasy : ''}`}
          >
            🔮 Fantasy Hub
          </button>
        </div>

        {/* Dynamic Tagline Banner */}
        <div className={styles.taglineBanner}>
          {activeTagline()}
        </div>

        <div className={styles.mainLayout}>
          
          {/* Left Column: Feed and inbox requests */}
          <div className={styles.feedSection}>
            
            {/* Feed Loader */}
            {loading ? (
              <div className={styles.loadingContainer}>
                <div className={styles.spinner} />
                <p>Loading real connections...</p>
              </div>
            ) : !user ? (
              <div className={styles.loginRequiredFeed}>
                <div className={styles.lockIcon}>🔒</div>
                <h3>Authentication Required</h3>
                <p>Sign in to view house party events, real meetup requests, and match with verified local members.</p>
                <button type="button" className="btn-neon" onClick={() => setShowLogin(true)} style={{ marginTop: '16px' }}>
                  Sign In with Google
                </button>
              </div>
            ) : (
              <>
                {/* Meet Feed list */}
                {activeTab === 'meet' && (
                  realMeetPosts.length === 0 ? (
                    <div className={styles.emptyFeed}>
                      <h3>No active Meet posts right now</h3>
                      <p>Be the first to publish a meetup purpose in your city!</p>
                    </div>
                  ) : (
                    realMeetPosts.map(post => (
                      <div key={post.id} className={`${styles.feedCard} ${post.premium ? styles.premiumCardBackground : ''}`}>
                        <div className={styles.userHeader}>
                          <div className={styles.userInfo}>
                            <img 
                              src={`/avatars/${post.userAvatar || 'av1'}.png`} 
                              alt="Avatar" 
                              className={`${styles.avatar} ${post.premium ? styles.avatarPremium : ''}`} 
                            />
                            <div className={styles.userNameContainer}>
                              <span className={styles.userName}>
                                {post.userName}
                                {post.gender?.startsWith('f') ? ' ♀️' : ' ♂️'}
                                {post.premium && ' 👑'}
                                {(post.gender?.startsWith('m') || post.verified) && <VerifiedBadge />}
                              </span>
                              <span className={styles.userAge}>{post.age} Yrs old • <span style={{ color: '#00E5FF', fontWeight: 'bold', fontSize: '11px' }}>{post.sexPreference?.toUpperCase() || 'STRAIGHT'}</span></span>
                            </div>
                          </div>
                          <span className={styles.cityBadge}>📍 {post.city}</span>
                        </div>

                        <span className={styles.fieldHeading}>PURPOSE</span>
                        <div className={styles.tagPill + ' ' + styles.realMeetTag}>{post.purpose}</div>

                        <span className={styles.fieldHeading}>VENUE</span>
                        <p className={styles.descriptionBox}>🏢 {post.location}</p>

                        <span className={styles.fieldHeading}>SCHEDULED TIME</span>
                        <div className={styles.infoRow}>
                          <Clock size={14} />
                          <span>{post.time}</span>
                        </div>

                        <span className={styles.fieldHeading}>ABOUT MEET</span>
                        <p className={styles.descriptionBox}>{post.description}</p>

                        {/* Actions Row */}
                        <div className={styles.requestRow}>
                          {post.userId === user.uid ? (
                            <button className="btn-neon" onClick={() => handleDeletePost(post.id)} style={{ padding: '6px 12px', background: 'rgba(239, 68, 68, 0.15)', borderColor: '#EF4444', color: '#EF4444' }}>
                              <Trash2 size={14} /> Delete Post
                            </button>
                          ) : (
                            requests.some(r => r.fromUserId === user.uid && r.postId === post.id) ? (
                              <span className={styles.requestedIndicator}>
                                <Check size={14} /> Requested to Meet
                              </span>
                            ) : (
                              <button className="btn-neon" onClick={() => handleSendRequest(post)}>
                                📩 Send Meet Request
                              </button>
                            )
                          )}
                        </div>
                      </div>
                    ))
                  )
                )}

                {/* Party Feed list */}
                {activeTab === 'party' && (
                  partyPosts.length === 0 ? (
                    <div className={styles.emptyFeed}>
                      <h3>No social parties right now</h3>
                      <p>Hosting a party? Add your event venue and invite verified members now!</p>
                    </div>
                  ) : (
                    partyPosts.map(post => (
                      <div key={post.id} className={`${styles.feedCard} ${post.premium ? styles.premiumCardBackground : ''}`}>
                        <div className={styles.userHeader}>
                          <div className={styles.userInfo}>
                            <img 
                              src={`/avatars/${post.hostAvatar || 'av1'}.png`} 
                              alt="Avatar" 
                              className={`${styles.avatar} ${post.premium ? styles.avatarPremium : ''}`} 
                            />
                            <div className={styles.userNameContainer}>
                              <span className={styles.userName}>
                                {post.hostName}
                                {post.gender?.startsWith('f') ? ' ♀️' : ' ♂️'}
                                {post.premium && ' 👑'}
                                {(post.gender?.startsWith('m') || post.verified) && <VerifiedBadge />}
                              </span>
                              <span className={styles.userAge}>{post.hostAge} Yrs old (Host)</span>
                            </div>
                          </div>
                          <span className={styles.cityBadge}>👥 Max {post.capacity} Guests</span>
                        </div>

                        <span className={styles.fieldHeading}>EVENT TITLE</span>
                        <div className={styles.tagPill + ' ' + styles.partyTag}>{post.purpose}</div>

                        <span className={styles.fieldHeading}>PARTY VENUE</span>
                        <p className={styles.descriptionBox}>📍 {post.venue}</p>

                        <span className={styles.fieldHeading}>SCHEDULED TIME</span>
                        <div className={styles.infoRow} style={{ marginBottom: '10px' }}>
                          <Clock size={14} />
                          <span>{post.partyTime}</span>
                        </div>

                        <div className={styles.infoRow}>
                          <Users size={14} />
                          <span>Target Audience: <b>{post.targetGender}</b></span>
                        </div>

                        {/* Guest List and Announcement Blocks */}
                        {post.hostUserId === user.uid ? (
                          <div className={styles.hostPartyControls}>
                            <div className={styles.controlRow}>
                              <label>Guest List Visibility:</label>
                              <select 
                                className={styles.selectInputSmall}
                                value={post.listVisibility || 'PRIVATE'}
                                onChange={(e) => handleUpdateVisibility(post.id, e.target.value)}
                              >
                                <option value="PRIVATE">Private (Only Accepted)</option>
                                <option value="PUBLIC">Public (Everyone)</option>
                              </select>
                            </div>
                            <div className={styles.announcementPublisher}>
                              <input 
                                type="text"
                                placeholder="Broadcast a party announcement..."
                                value={announcementTexts[post.id] || ''}
                                onChange={(e) => setAnnouncementTexts(prev => ({ ...prev, [post.id]: e.target.value }))}
                              />
                              <button onClick={() => handlePostAnnouncement(post.id)}>Broadcast</button>
                            </div>
                          </div>
                        ) : null}

                        {/* Display Accepted Guests List */}
                        {((post.listVisibility === 'PUBLIC') || 
                          (post.hostUserId === user.uid) || 
                          partyMembers[post.id]?.some(m => m.userId === user.uid)) && (
                          <div className={styles.membersSection}>
                            <h4>👥 Guest List ({partyMembers[post.id]?.length || 0} accepted)</h4>
                            <div className={styles.membersGrid}>
                              {(partyMembers[post.id] || []).map(member => (
                                <div key={member.userId} className={styles.memberAvatarWrapper}>
                                  <img 
                                    src={`/avatars/${member.avatar || 'av1'}.png`} 
                                    alt={member.name}
                                    title={member.name}
                                  />
                                  <span>{member.name.split(' ')[0]}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Display Announcements */}
                        {((post.hostUserId === user.uid) || 
                          partyMembers[post.id]?.some(m => m.userId === user.uid)) && 
                          partyAnnouncements[post.id]?.length > 0 && (
                          <div className={styles.announcementsSection}>
                            <h4>📢 Broadcast Announcements</h4>
                            <ul>
                              {(partyAnnouncements[post.id] || []).map(ann => (
                                <li key={ann.id}>
                                  <span>{ann.text}</span>
                                  <small>{new Date(ann.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</small>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Actions Row */}
                        <div className={styles.requestRow} style={{ gap: '10px', display: 'flex', alignItems: 'center' }}>
                          {post.hostUserId === user.uid ? (
                            <button className="btn-neon" onClick={() => handleDeletePost(post.id)} style={{ flex: 1, padding: '6px 12px', background: 'rgba(239, 68, 68, 0.15)', borderColor: '#EF4444', color: '#EF4444' }}>
                              <Trash2 size={14} /> Delete Event
                            </button>
                          ) : (
                            <>
                              {requests.some(r => r.fromUserId === user.uid && r.postId === post.id) ? (
                                <span className={styles.requestedIndicator} style={{ flex: 1 }}>
                                  <Check size={14} /> Requested to Join
                                </span>
                              ) : (
                                <button className="btn-neon" onClick={() => handleSendRequest(post)} style={{ flex: 1 }}>
                                  📩 Request Invite
                                </button>
                              )}
                              <button 
                                className="btn-neon" 
                                onClick={() => toggleSaveParty(post.id)}
                                style={{ 
                                  padding: '8px', 
                                  background: savedPartyIds.includes(post.id) ? 'rgba(84, 214, 210, 0.15)' : 'transparent',
                                  borderColor: savedPartyIds.includes(post.id) ? 'var(--neon-cyan)' : 'var(--glass-border)'
                                }}
                              >
                                <Bookmark size={16} color={savedPartyIds.includes(post.id) ? 'var(--neon-cyan)' : '#FFFFFF'} />
                              </button>
                            </>
                          )}
                        </div>
                      </div>
                    ))
                  )
                )}

                {/* Fantasy Feed list */}
                {activeTab === 'fantasy' && (
                  fantasyPosts.length === 0 ? (
                    <div className={styles.emptyFeed}>
                      <h3>No fantasies shared yet</h3>
                      <p>Share your matching fantasy text safely under full encryption.</p>
                    </div>
                  ) : (
                    fantasyPosts.map(post => (
                      <div key={post.id} className={`${styles.feedCard} ${post.premium ? styles.premiumCardBackground : ''}`}>
                        <div className={styles.userHeader}>
                          <div className={styles.userInfo}>
                            <img 
                              src={`/avatars/${post.userAvatar || 'av1'}.png`} 
                              alt="Avatar" 
                              className={`${styles.avatar} ${post.premium ? styles.avatarPremium : ''}`} 
                            />
                            <div className={styles.userNameContainer}>
                              <span className={styles.userName}>
                                {post.userName}
                                {post.gender?.startsWith('f') ? ' ♀️' : ' ♂️'}
                                {post.premium && ' 👑'}
                                {(post.gender?.startsWith('m') || post.verified) && <VerifiedBadge />}
                              </span>
                              <span className={styles.userAge}>{post.age} Yrs old • {post.relationshipStatus}</span>
                            </div>
                          </div>
                        </div>

                        <span className={styles.fieldHeading}>INTERESTS & VIBE</span>
                        <div className={styles.tagPill + ' ' + styles.fantasyTag}>✨ {post.interests}</div>

                        <span className={styles.fieldHeading}>MY FANTASY</span>
                        <div className={styles.fantasyQuote}>
                          "{post.description}"
                        </div>

                        {/* Actions Row */}
                        <div className={styles.requestRow}>
                          {post.userId === user.uid ? (
                            <button className="btn-neon" onClick={() => handleDeletePost(post.id)} style={{ padding: '6px 12px', background: 'rgba(239, 68, 68, 0.15)', borderColor: '#EF4444', color: '#EF4444' }}>
                              <Trash2 size={14} /> Delete Post
                            </button>
                          ) : (
                            requests.some(r => r.fromUserId === user.uid && r.postId === post.id) ? (
                              <span className={styles.requestedIndicator}>
                                <Check size={14} /> Vibe Connection Requested
                              </span>
                            ) : (
                              <button className="btn-neon" onClick={() => handleSendRequest(post)}>
                                ⚡ Connect Vibe
                              </button>
                            )
                          )}
                        </div>
                      </div>
                    ))
                  )
                )}
              </>
            )}
          </div>

          {/* Right Column: Composer and inbox requests list */}
          <div className={styles.composerSection}>
            
            {user && (
              <>
                {/* Active Form Composer */}
                <div className={styles.composerCard}>
                  <h3>
                    <PlusCircle size={20} className="glowIcon" />
                    {activeTab === 'meet' ? 'Host a Meetup' : activeTab === 'party' ? 'Host a Party' : 'Post your Fantasy'}
                  </h3>
                  <form onSubmit={handleCreatePost}>
                    
                    {/* Render Meet Form */}
                    {activeTab === 'meet' && (
                      <>
                        <div className={styles.formGroup}>
                          <label>Meet Purpose / Vibe</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. Coffee & Startup discussions" 
                            value={meetPurpose}
                            onChange={e => setMeetPurpose(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup}>
                          <label>Meet Location / Venue</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. Starbucks, Connaught Place" 
                            value={meetLocation}
                            onChange={e => setMeetLocation(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup}>
                          <label>Date &amp; Time</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. Today at 6:30 PM" 
                            value={meetTime}
                            onChange={e => setMeetTime(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup}>
                          <label>Description / Details</label>
                          <textarea 
                            className={styles.textInput} 
                            placeholder="Provide details about who you want to meet..." 
                            rows={3}
                            value={meetDescription}
                            onChange={e => setMeetDescription(e.target.value)}
                          />
                        </div>
                      </>
                    )}

                    {/* Render Party Form */}
                    {activeTab === 'party' && (
                      <>
                        <div className={styles.formGroup}>
                          <label>Party Event Title</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. House party / DJ Night" 
                            value={partyTitle}
                            onChange={e => setPartyTitle(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup}>
                          <label>Venue Details</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. Penthouse Suite, Cyber City" 
                            value={partyVenue}
                            onChange={e => setPartyVenue(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup}>
                          <label>Date &amp; Scheduled Time</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. Saturday Night 8 PM onwards" 
                            value={partyTime}
                            onChange={e => setPartyTime(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup} style={{ display: 'flex', gap: '10px' }}>
                          <div style={{ flex: 1 }}>
                            <label>Capacity</label>
                            <select 
                              className={styles.selectInput}
                              value={partyCapacity}
                              onChange={e => setPartyCapacity(e.target.value)}
                            >
                              <option value="5">5 Max</option>
                              <option value="10">10 Max</option>
                              <option value="20">20 Max</option>
                              <option value="50">50 Max</option>
                            </select>
                          </div>
                          <div style={{ flex: 1.2 }}>
                            <label>Target Audience</label>
                            <select 
                              className={styles.selectInput}
                              value={partyTargetGender}
                              onChange={e => setPartyTargetGender(e.target.value)}
                            >
                              <option value="Everyone">Everyone</option>
                              <option value="Female Only">Female Only</option>
                              <option value="Male Only">Male Only</option>
                            </select>
                          </div>
                        </div>
                      </>
                    )}

                    {/* Render Fantasy Form */}
                    {activeTab === 'fantasy' && (
                      <>
                        <div className={styles.formGroup}>
                          <label>Relationship Status</label>
                          <select 
                            className={styles.selectInput}
                            value={fantasyStatus}
                            onChange={e => setFantasyStatus(e.target.value)}
                          >
                            <option value="Single">Single</option>
                            <option value="Married">Married</option>
                            <option value="Divorced">Divorced</option>
                            <option value="In Relationship">In Relationship</option>
                          </select>
                        </div>
                        <div className={styles.formGroup}>
                          <label>Interests &amp; Vibe Tag</label>
                          <input 
                            type="text" 
                            className={styles.textInput} 
                            placeholder="e.g. Secret conversations, long drives" 
                            value={fantasyInterests}
                            onChange={e => setFantasyInterests(e.target.value)}
                          />
                        </div>
                        <div className={styles.formGroup}>
                          <label>My Fantasy Description</label>
                          <textarea 
                            className={styles.textInput} 
                            placeholder="Share what type of vibe you are looking for..." 
                            rows={4}
                            value={fantasyDescription}
                            onChange={e => setFantasyDescription(e.target.value)}
                            maxLength={200}
                          />
                          <span className={styles.characterCount}>{fantasyDescription.length}/200</span>
                        </div>
                      </>
                    )}

                    <button 
                      type="submit" 
                      className="btn-neon" 
                      style={{ width: '100%', marginTop: '10px' }}
                      disabled={posting}
                    >
                      <Send size={16} /> {posting ? 'Publishing...' : 'Post Now'}
                    </button>
                  </form>
                </div>

                {/* Inbox Requests Section (Received Requests) */}
                <h3 id="realmeet-inbox" className={styles.requestsHeader}>
                  📩 Real Meet Inbox ({requests.filter(r => r.toUserId === user.uid).length})
                </h3>
                <div className={styles.requestsList}>
                  {requests.filter(r => r.toUserId === user.uid).length === 0 ? (
                    <p style={{ fontStyle: 'italic', fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center', padding: '10px 0' }}>
                      No incoming meet requests yet.
                    </p>
                  ) : (
                    requests.filter(r => r.toUserId === user.uid).map(req => (
                      <div key={req.id} className={styles.requestInboxCard}>
                        <div className={styles.requestInboxHeader}>
                          <div className={styles.requestProfile}>
                            <img src={`/avatars/${req.fromUserAvatar || 'av1'}.png`} alt="Avatar" className={styles.requestAvatar} />
                            <div>
                              <span className={styles.requestSenderName}>
                                {req.fromUserName}
                                {req.fromUserGender?.startsWith('f') ? ' ♀️' : ' ♂️'}
                                {req.fromUserVerified && <VerifiedBadge />}
                              </span>
                              <div className={styles.requestMeta}>Wants to join your activity</div>
                            </div>
                          </div>
                        </div>
                        <div className={styles.requestPurpose}>
                          🎯 <b>Activity Title:</b> {req.postTitle}
                        </div>
                        {req.status === 'requested' ? (
                          <div className={styles.actionsRow}>
                            <button className={styles.acceptBtn} onClick={() => handleUpdateStatus(req.id, 'accepted')}>
                              Accept Request
                            </button>
                            <button className={styles.rejectBtn} onClick={() => handleUpdateStatus(req.id, 'rejected')}>
                              Reject
                            </button>
                          </div>
                        ) : (
                          <div className={styles.acceptedBadge} style={{
                            background: req.status === 'accepted' ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.1)',
                            borderColor: req.status === 'accepted' ? '#10B981' : '#EF4444',
                            color: req.status === 'accepted' ? '#10B981' : '#EF4444',
                          }}>
                            {req.status === 'accepted' ? '✓ Accepted Invitation' : '✕ Rejected Invitation'}
                          </div>
                        )}
                      </div>
                    ))
                  )}
                </div>

                {/* Saved Parties Section */}
                <h3 className={styles.requestsHeader} style={{ marginTop: '24px' }}>
                  🔖 Saved Parties ({savedParties.length})
                </h3>
                <div className={styles.requestsList} style={{ maxHeight: '250px', overflowY: 'auto' }}>
                  {savedParties.length === 0 ? (
                    <p style={{ fontStyle: 'italic', fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center', padding: '10px 0' }}>
                      No saved parties yet.
                    </p>
                  ) : (
                    savedParties.map(savedPost => (
                      <div key={savedPost.id} className={styles.requestInboxCard} style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px', borderRadius: '8px', marginBottom: '10px' }}>
                        <div className={styles.requestInboxHeader} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <div className={styles.requestProfile} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                            <img src={`/avatars/${savedPost.hostAvatar || 'av1'}.png`} alt="Avatar" className={styles.requestAvatar} style={{ width: '28px', height: '28px', borderRadius: '50%' }} />
                            <div>
                              <span className={styles.requestSenderName} style={{ fontSize: '0.85rem', fontWeight: 'bold' }}>
                                {savedPost.hostName}
                                {savedPost.gender?.startsWith('f') ? ' ♀️' : ' ♂️'}
                                {savedPost.verified && <VerifiedBadge />}
                              </span>
                              <div className={styles.requestMeta} style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>📍 {savedPost.venue}</div>
                            </div>
                          </div>
                          <button 
                            onClick={() => toggleSaveParty(savedPost.id)}
                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#EF4444', fontSize: '0.75rem', fontWeight: 'bold' }}
                          >
                            Remove
                          </button>
                        </div>
                        <div className={styles.requestPurpose} style={{ marginTop: '8px', fontSize: '0.8rem', color: '#FFFFFF' }}>
                          🎉 <b>Title:</b> {savedPost.purpose} <br />
                          ⏰ <b>Time:</b> {savedPost.partyTime}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>

    {/* Bell Notifications Drawer */}
    {showNotificationsDrawer && (
      <div className="modal-overlay" style={{ zIndex: 1000 }} onClick={() => setShowNotificationsDrawer(false)}>
        <div className={styles.notificationsDrawer} onClick={e => e.stopPropagation()} style={{ width: '400px', height: '100%', position: 'fixed', right: 0, top: 0, background: '#09090D', borderLeft: '1px solid #1A1A24', padding: '24px', display: 'flex', flexDirection: 'column', color: '#FFFFFF', overflowY: 'auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 'bold' }}>🔔 Community Alerts</h3>
            <button onClick={() => setShowNotificationsDrawer(false)} style={{ background: 'none', border: 'none', color: '#FFFFFF', cursor: 'pointer' }}>
              <CloseIcon size={20} />
            </button>
          </div>

          {/* Alert Tabs */}
          <div className={styles.tabsContainer} style={{ marginBottom: '16px', display: 'flex', gap: '8px' }}>
            {['meet', 'fantasy', 'party'].map(tab => (
              <button
                key={tab}
                onClick={() => setNotifTab(tab)}
                style={{
                  flex: 1,
                  padding: '8px 12px',
                  background: notifTab === tab ? 'var(--neon-cyan)' : 'rgba(255,255,255,0.05)',
                  color: notifTab === tab ? '#000000' : '#8E8E93',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontWeight: 'bold',
                  fontSize: '0.8rem',
                  textTransform: 'capitalize'
                }}
              >
                {tab}
              </button>
            ))}
          </div>

          <div style={{ flex: 1, overflowY: 'auto' }}>
            {notifications.filter(n => {
              const postType = n.postType || '';
              if (notifTab === 'meet' && postType === 'REAL_MEET') return true;
              if (notifTab === 'fantasy' && postType === 'FANTASY') return true;
              if (notifTab === 'party' && (postType === 'PARTY' || n.type === 'party_announcement')) return true;
              return false;
            }).length === 0 ? (
              <p style={{ fontStyle: 'italic', textAlign: 'center', color: '#8E8E93', marginTop: '40px' }}>No notifications here.</p>
            ) : (
              notifications.filter(n => {
                const postType = n.postType || '';
                if (notifTab === 'meet' && postType === 'REAL_MEET') return true;
                if (notifTab === 'fantasy' && postType === 'FANTASY') return true;
                if (notifTab === 'party' && (postType === 'PARTY' || n.type === 'party_announcement')) return true;
                return false;
              }).map(notif => (
                <div key={notif.id} className={styles.requestInboxCard} style={{ background: 'rgba(255,255,255,0.02)', padding: '12px', borderRadius: '8px', marginBottom: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <img 
                      src={`/avatars/${notif.triggeringUser?.avatar || 'av1'}.png`} 
                      alt="Avatar" 
                      style={{ width: '36px', height: '36px', borderRadius: '50%' }}
                    />
                    <div style={{ flex: 1 }}>
                      <p style={{ margin: 0, fontSize: '0.85rem' }}>
                        <b>{notif.triggeringUser?.name || 'Someone'}</b>{' '}
                        {notif.type === 'realmeet_request' ? 'requested to connect on your post: ' : 
                         notif.type === 'realmeet_accepted' ? 'accepted your request for post: ' : 
                         'posted an announcement for party: '}
                        <b>"{notif.postTitle || 'Community Post'}"</b>
                      </p>
                      <small style={{ color: '#8E8E93' }}>
                        {new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </small>
                    </div>
                  </div>
                  {notif.type === 'realmeet_request' && notif.friendshipStatus === 'PENDING' && (
                    <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                      <button 
                        onClick={() => handleUpdateStatus(notif.friendRequestId, 'ACCEPTED')}
                        style={{ flex: 1, padding: '4px 8px', background: '#10B981', color: '#000000', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 'bold' }}
                      >
                        Accept
                      </button>
                      <button 
                        onClick={() => handleUpdateStatus(notif.friendRequestId, 'REJECTED')}
                        style={{ flex: 1, padding: '4px 8px', background: '#EF4444', color: '#FFFFFF', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 'bold' }}
                      >
                        Decline
                      </button>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    )}

    {showPreferencePopup && (
      <div className="modal-overlay" style={{ zIndex: 1000 }}>
        <div className={styles.preferenceModal} onClick={e => e.stopPropagation()}>
          <button className={styles.closeBtn} onClick={() => {
            localStorage.setItem('sex_preference_selected_v2', 'true');
            setShowPreferencePopup(false);
          }}>
            <CloseIcon size={20} />
          </button>
          <h2>Select Primary Preference</h2>
          <p className={styles.preferenceSubtitle}>
            Choose your primary sex preference (gay, lesbian, straight or transgender). You can change this later in your profile.
          </p>
          
          <div className={styles.preferenceGrid}>
            {[
              { val: 'Straight', label: 'Straight 💑', desc: 'Interested in opposite gender' },
              { val: 'Gay', label: 'Gay 👨‍❤️‍👨', desc: 'Interested in same gender (Male)' },
              { val: 'Lesbian', label: 'Lesbian 👩‍❤️‍👩', desc: 'Interested in same gender (Female)' },
              { val: 'Transgender', label: 'Transgender ⚧️', desc: 'Transgender community & matches' },
            ].map(item => (
              <div 
                key={item.val}
                className={`${styles.preferenceCard} ${selectedPreference === item.val ? styles.activePreferenceCard : ''}`}
                style={{ 
                  background: selectedPreference === item.val ? 'rgba(84, 214, 210, 0.12)' : 'rgba(255, 255, 255, 0.02)',
                  borderColor: selectedPreference === item.val ? 'var(--neon-cyan)' : 'var(--glass-border)'
                }}
                onClick={() => setSelectedPreference(item.val)}
              >
                <h3>{item.label}</h3>
                <p>{item.desc}</p>
              </div>
            ))}
          </div>
          
          <button 
            className="btn-neon" 
            style={{ width: '100%', marginTop: '20px' }}
            disabled={!selectedPreference}
            onClick={async () => {
              try {
                const res = await updateMe({ sexPreference: selectedPreference });
                if (res.ok) {
                  localStorage.setItem('sex_preference_selected_v2', 'true');
                  setShowPreferencePopup(false);
                  window.location.reload();
                }
              } catch (err) {
                console.error('Failed to update preference:', err);
              }
            }}
          >
            Confirm Preference
          </button>
        </div>
      </div>
    )}
    </>
  );
}

export default function RealMeetPage() {
  return (
    <Suspense fallback={
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#060612', color: 'rgba(255, 255, 255, 0.7)' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid rgba(0, 229, 255, 0.1)', borderTopColor: '#00E5FF', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
        <style dangerouslySetInnerHTML={{__html: `
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}} />
      </div>
    }>
      <RealMeetContent />
    </Suspense>
  );
}
