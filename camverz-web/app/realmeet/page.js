'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '../../lib/auth-context';
import { 
  getRealMeetFeed, 
  createRealMeetPost, 
  deleteRealMeetPost, 
  getRealMeetRequests, 
  sendRealMeetRequest, 
  updateRealMeetRequestStatus,
  updateMe
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
  ShieldAlert 
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

export default function RealMeetPage() {
  const { user, userData, loading: authLoading, setShowLogin, setShowAppRedirect } = useAuth();
  
  // Selected Tab: 'meet' | 'party' | 'fantasy'
  const [activeTab, setActiveTab] = useState('meet');
  const [loading, setLoading] = useState(true);
  
  // Feeds data
  const [realMeetPosts, setRealMeetPosts] = useState([]);
  const [partyPosts, setPartyPosts] = useState([]);
  const [fantasyPosts, setFantasyPosts] = useState([]);
  const [requests, setRequests] = useState([]);
  
  // Device Lock State
  const [isAndroid, setIsAndroid] = useState(false);

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
      setIsAndroid(/android/i.test(ua));
    }
  }, []);

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
      }
      
      // Requests
      const reqRes = await getRealMeetRequests();
      if (reqRes.ok) {
        setRequests(reqRes.requests || []);
      }
    } catch (err) {
      console.error('Error loading Real Meet data:', err);
    } finally {
      setLoading(false);
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

  // 6. Request to Meet Handler
  const handleSendRequest = async (post) => {
    checkLockAndExecute('messages', async () => {
      // Prevent multiple requests
      const targetUserId = post.userId || post.hostUserId;
      const alreadyRequested = requests.some(r => r.fromUserId === user.uid && r.toUserId === targetUserId);
      if (alreadyRequested) {
        alert('You have already sent a request to this user.');
        return;
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
        <header className={styles.header}>
          <h1 className="neon-text">Real Meet Hub</h1>
          <p>Facilitating authentic real-world connections, social house parties, and shared fantasies.</p>
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
                              <span className={styles.userAge}>{post.age} Yrs old</span>
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

                        {/* Actions Row */}
                        <div className={styles.requestRow}>
                          {post.hostUserId === user.uid ? (
                            <button className="btn-neon" onClick={() => handleDeletePost(post.id)} style={{ padding: '6px 12px', background: 'rgba(239, 68, 68, 0.15)', borderColor: '#EF4444', color: '#EF4444' }}>
                              <Trash2 size={14} /> Delete Event
                            </button>
                          ) : (
                            requests.some(r => r.fromUserId === user.uid && r.postId === post.id) ? (
                              <span className={styles.requestedIndicator}>
                                <Check size={14} /> Requested to Join
                              </span>
                            ) : (
                              <button className="btn-neon" onClick={() => handleSendRequest(post)}>
                                📩 Request Invite
                              </button>
                            )
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
                              <option value="Couples Only">Couples Only</option>
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
                <h3 className={styles.requestsHeader}>
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
              </>
            )}
          </div>
        </div>
      </div>
    </div>

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
