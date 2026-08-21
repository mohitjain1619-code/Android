'use client';
import { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../lib/auth-context';
import { updateUser, getFollowersCount, getFollowingCount } from '../../lib/firestore';
import { useRouter } from 'next/navigation';
import { Edit3, MapPin, Calendar, Shield, ShieldCheck, Camera, Save, X, LogOut, Heart, Star, Music, Briefcase, BookOpen, Dumbbell, Coffee, Globe, Smile, Crown, Zap, AlertCircle } from 'lucide-react';
import { getSubscriptionStatus } from '../../lib/subscription';
import styles from './page.module.css';

const AVATARS = Array.from({ length: 15 }, (_, i) => `av${i + 1}`);

const INTEREST_OPTIONS = ['Music', 'Travel', 'Gaming', 'Fitness', 'Photography', 'Art', 'Cooking', 'Movies', 'Reading', 'Technology', 'Dancing', 'Fashion', 'Sports', 'Nature', 'Animals', 'Meditation'];

const RELATIONSHIP_GOALS = ['Friendship', 'Dating', 'Long-term', 'Networking', 'Just Chatting'];

export default function ProfilePage() {
  const { user, userData, loading, signOut, refreshUserData, setShowVerification } = useAuth();
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [showAvatarPicker, setShowAvatarPicker] = useState(false);
  const [showGenderWarning, setShowGenderWarning] = useState(false);
  const [followers, setFollowers] = useState(0);
  const [following, setFollowing] = useState(0);
  const [form, setForm] = useState({
    name: '', bio: '', avatar: '', city: '',
    interests: [], relationshipGoal: '', zodiac: '', height: '',
    education: '', work: '', languages: '', smoking: '', drinking: '', exercise: '',
    sexPreference: 'Straight',
  });
  const [saving, setSaving] = useState(false);

  const prevUserRef = useRef(user);

  useEffect(() => {
    if (loading) return;

    if (!user) {
      if (prevUserRef.current) {
        // User actively signed out: redirect to homepage cleanly without prompting login
        router.push('/');
      } else {
        // User was already logged out: redirect to homepage and trigger login modal
        router.push('/?login=true');
      }
      return;
    }

    prevUserRef.current = user;

    if (userData) {
      setForm({
        name: userData.name || '',
        bio: userData.bio || '',
        avatar: userData.avatar || 'av1',
        city: userData.city || '',
        interests: userData.webProfile?.interests || [],
        relationshipGoal: userData.webProfile?.relationshipGoal || '',
        zodiac: userData.webProfile?.zodiac || '',
        height: userData.webProfile?.height || '',
        education: userData.webProfile?.education || '',
        work: userData.webProfile?.work || '',
        languages: userData.webProfile?.languages || '',
        smoking: userData.webProfile?.smoking || '',
        drinking: userData.webProfile?.drinking || '',
        exercise: userData.webProfile?.exercise || '',
        sexPreference: userData.sexPreference || 'Straight',
      });
      getFollowersCount(user.uid).then(setFollowers);
      getFollowingCount(user.uid).then(setFollowing);
    }
  }, [user, userData, loading]);

  const calculateAge = (dob) => {
    if (!dob) return '';
    const parts = dob.split('/');
    if (parts.length !== 3) return '';
    const birthDate = new Date(parts[2], parts[1] - 1, parts[0]);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
    return age;
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateUser(user.uid, {
        name: form.name,
        bio: form.bio,
        avatar: form.avatar,
        city: form.city,
        sexPreference: form.sexPreference,
        webProfile: {
          interests: form.interests,
          relationshipGoal: form.relationshipGoal,
          zodiac: form.zodiac,
          height: form.height,
          education: form.education,
          work: form.work,
          languages: form.languages,
          smoking: form.smoking,
          drinking: form.drinking,
          exercise: form.exercise,
        },
      });
      await refreshUserData();
      setEditing(false);
    } catch (e) {
      console.error(e);
    }
    setSaving(false);
  };

  const executeGenderChange = async () => {
    const newGender = userData.gender?.toLowerCase()?.trim() === 'male' ? 'female' : 'male';
    setSaving(true);
    try {
      await updateUser(user.uid, {
        gender: newGender,
        verified: newGender === 'male', // Male is auto-verified, Female is unverified
      });
      await refreshUserData();
      setShowGenderWarning(false);
    } catch (e) {
      console.error('Error changing profile gender:', e);
    }
    setSaving(false);
  };

  const toggleInterest = (interest) => {
    setForm(prev => ({
      ...prev,
      interests: prev.interests.includes(interest)
        ? prev.interests.filter(i => i !== interest)
        : [...prev.interests, interest],
    }));
  };

  if (!userData) {
    return (
      <div className={styles.page}>
        <div className={styles.loading}>
          <div className="skeleton" style={{ width: 100, height: 100, borderRadius: '50%' }} />
          <div className="skeleton" style={{ width: 200, height: 24, marginTop: 16 }} />
          <div className="skeleton" style={{ width: 150, height: 16, marginTop: 8 }} />
        </div>
      </div>
    );
  }

  const age = calculateAge(userData.dob);
  const subStatus = getSubscriptionStatus(userData);

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.avatarSection}>
            <div className={styles.avatarWrapper} onClick={() => editing && setShowAvatarPicker(true)}>
              <img src={`/avatars/${form.avatar || 'av1'}.png`} alt="Avatar" className={styles.avatar} />
              {editing && <div className={styles.avatarOverlay}><Camera size={20} /></div>}
              {userData.verified && <div className={styles.verifiedBadge}><ShieldCheck size={14} /></div>}
            </div>

            {editing ? (
              <input className="input-glass" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Your name" style={{ textAlign: 'center', maxWidth: 250 }} />
            ) : (
              <h1 className={styles.userName}>{userData.name}</h1>
            )}

            <div className={styles.metaRow}>
              {userData.gender && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span className={styles.genderBadge} style={{ color: userData.gender === 'male' ? 'var(--neon-blue)' : 'var(--neon-pink)', margin: 0 }}>
                    {userData.gender === 'male' ? '♂' : '♀'} {userData.gender}
                  </span>
                  <button 
                    onClick={() => setShowGenderWarning(true)}
                    style={{ 
                      background: 'rgba(255,255,255,0.05)', 
                      border: '1px solid rgba(255,255,255,0.1)', 
                      borderRadius: '4px',
                      padding: '2px 8px',
                      fontSize: '0.7rem',
                      color: 'rgba(255,255,255,0.6)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      fontWeight: 'bold'
                    }}
                  >
                    Change
                  </button>
                </div>
              )}
              {age && <span className={styles.metaItem}>{age} yrs</span>}
              {userData.city && <span className={styles.metaItem}><MapPin size={12} /> {userData.city}</span>}
            </div>

            <div className={styles.statsRow}>
              <div><strong>{followers}</strong><span>Followers</span></div>
              <div className={styles.statDivider} />
              <div><strong>{following}</strong><span>Following</span></div>
            </div>
          </div>

          <div className={styles.headerActions}>
            {editing ? (
              <>
                <button className="btn-neon" onClick={handleSave} disabled={saving}>
                  <Save size={16} /> {saving ? 'Saving...' : 'Save'}
                </button>
                <button className="btn-glass" onClick={() => setEditing(false)}><X size={16} /> Cancel</button>
              </>
            ) : (
              <>
                <button className="btn-neon" onClick={() => setEditing(true)}><Edit3 size={16} /> Edit Profile</button>
                <button className="btn-glass" onClick={() => { signOut(); router.push('/'); }}><LogOut size={16} /> Sign Out</button>
              </>
            )}
          </div>
        </div>

        {/* Luxury Subscription Status Card */}
        <div className={styles.subscriptionCard} style={{ borderColor: subStatus.badgeColor }}>
          <div className={styles.subCardMain}>
            <div className={styles.subIconWrapper} style={{ background: `rgba(${subStatus.isAdFree ? '0, 229, 255' : '189, 0, 255'}, 0.15)` }}>
              <Crown size={28} style={{ color: subStatus.badgeColor }} />
            </div>
            <div className={styles.subCardInfo}>
              <div className={styles.subCardHeaderRow}>
                <h3 className={styles.subPlanTitle}>{subStatus.planName}</h3>
                <span className={styles.subBadge} style={{ background: subStatus.badgeColor }}>
                  {subStatus.badgeText}
                </span>
              </div>
              <p className={styles.subRemainingText}>
                {subStatus.hasActivePlan ? (
                  <>⏱️ Active Pass: <strong>{subStatus.remainingText}</strong></>
                ) : (
                  <>Unlock unlimited matching, HD video, and 100% ad-free experience.</>
                )}
              </p>
            </div>
          </div>
          <button 
            className="btn-neon" 
            onClick={() => router.push('/pricing')}
            style={{ padding: '10px 20px', fontSize: '0.88rem', flexShrink: 0 }}
          >
            <Zap size={16} /> {subStatus.hasActivePlan ? 'Extend / Upgrade Pass' : 'Get VIP Pass'}
          </button>
        </div>

        {/* Verification Banner */}
        {userData.gender === 'female' && !userData.verified && (
          <div className={styles.verificationBanner}>
            <div className={styles.bannerInfo}>
              <Shield className={styles.bannerIcon} size={24} style={{ color: 'var(--neon-pink)' }} />
              <div className={styles.bannerText}>
                <h4>Profile Verification Required</h4>
                <p>Verify your identity to unlock matching filters and get your verified badge.</p>
              </div>
            </div>
            <button className="btn-neon" onClick={() => setShowVerification(true)} style={{ padding: '10px 24px', fontSize: '0.9rem' }}>
              Verify Identity
            </button>
          </div>
        )}

        {/* Bio */}
        <div className={styles.section}>
          <h3><Smile size={18} /> About Me</h3>
          {editing ? (
            <textarea className="input-glass" value={form.bio} onChange={e => setForm({ ...form, bio: e.target.value })} placeholder="Tell the world about yourself..." rows={3} style={{ resize: 'vertical', width: '100%' }} />
          ) : (
            <p className={styles.bioText}>{userData.bio || 'No bio yet'}</p>
          )}
        </div>

        {/* Interests */}
        <div className={styles.section}>
          <h3><Star size={18} /> Interests</h3>
          <div className={styles.chipGrid}>
            {(editing ? INTEREST_OPTIONS : (form.interests.length ? form.interests : ['No interests added'])).map(i => (
              <button
                key={i}
                className={`chip ${form.interests.includes(i) ? 'active' : ''}`}
                onClick={() => editing && toggleInterest(i)}
                style={!editing ? { cursor: 'default' } : {}}
              >
                {i}
              </button>
            ))}
          </div>
        </div>

        {/* Relationship Goal */}
        <div className={styles.section}>
          <h3><Heart size={18} /> Looking For</h3>
          {editing ? (
            <div className={styles.chipGrid}>
              {RELATIONSHIP_GOALS.map(g => (
                <button key={g} className={`chip ${form.relationshipGoal === g ? 'active' : ''}`} onClick={() => setForm({ ...form, relationshipGoal: g })}>
                  {g}
                </button>
              ))}
            </div>
          ) : (
            <p className={styles.infoText}>{form.relationshipGoal || 'Not specified'}</p>
          )}
        </div>

        {/* Primary Sex Preference */}
        <div className={styles.section}>
          <h3><Crown size={18} /> Primary Sex Preference</h3>
          {editing ? (
            <div className={styles.chipGrid}>
              {['Straight', 'Gay', 'Lesbian', 'Transgender'].map(p => (
                <button key={p} className={`chip ${form.sexPreference === p ? 'active' : ''}`} onClick={() => setForm({ ...form, sexPreference: p })}>
                  {p}
                </button>
              ))}
            </div>
          ) : (
            <p className={styles.infoText}>{form.sexPreference || 'Straight'}</p>
          )}
        </div>

        {/* Lifestyle */}
        <div className={styles.section}>
          <h3><Coffee size={18} /> Lifestyle</h3>
          <div className={styles.detailGrid}>
            {[
              { key: 'zodiac', icon: Star, label: 'Zodiac', placeholder: 'e.g. Aries' },
              { key: 'height', icon: Dumbbell, label: 'Height', placeholder: "e.g. 5'10\"" },
              { key: 'education', icon: BookOpen, label: 'Education', placeholder: 'e.g. University' },
              { key: 'work', icon: Briefcase, label: 'Work', placeholder: 'e.g. Engineer' },
              { key: 'languages', icon: Globe, label: 'Languages', placeholder: 'e.g. English, Hindi' },
            ].map(d => (
              <div key={d.key} className={styles.detailItem}>
                <div className={styles.detailLabel}><d.icon size={14} /> {d.label}</div>
                {editing ? (
                  <input className="input-glass" value={form[d.key]} onChange={e => setForm({ ...form, [d.key]: e.target.value })} placeholder={d.placeholder} />
                ) : (
                  <div className={styles.detailValue}>{form[d.key] || 'Not specified'}</div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Custom ID */}
        <div className={styles.section}>
          <h3><Shield size={18} /> Account Info</h3>
          <div className={styles.detailGrid}>
            <div className={styles.detailItem}>
              <div className={styles.detailLabel}>Custom ID</div>
              <div className={styles.detailValue} style={{ fontFamily: 'monospace', color: 'var(--neon-cyan)' }}>{userData.customId || userData.uid?.substring(0, 8)}</div>
            </div>
            <div className={styles.detailItem}>
              <div className={styles.detailLabel}>Verified</div>
              <div className={styles.detailValue}>
                {userData.verified ? <span style={{ color: 'var(--neon-green)' }}>✓ Verified</span> : <span style={{ color: 'var(--text-muted)' }}>Not verified</span>}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Avatar Picker Modal */}
      {showAvatarPicker && (
        <div className="modal-overlay" onClick={() => setShowAvatarPicker(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <h3 style={{ marginBottom: 16 }} className="neon-text">Choose Avatar</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8 }}>
              {AVATARS.map(av => (
                <button
                  key={av}
                  onClick={() => { setForm({ ...form, avatar: av }); setShowAvatarPicker(false); }}
                  style={{
                    padding: 0, background: 'none', borderRadius: '50%', overflow: 'hidden', aspectRatio: 1,
                    border: form.avatar === av ? '2px solid var(--neon-cyan)' : '2px solid transparent',
                    boxShadow: form.avatar === av ? '0 0 15px rgba(0,229,255,0.4)' : 'none',
                    transition: 'all 0.2s',
                  }}
                >
                  <img src={`/avatars/${av}.png`} alt={av} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Gender Change Warning Modal */}
      {showGenderWarning && (
        <div className="modal-overlay" onClick={() => setShowGenderWarning(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()} style={{ border: '2px solid var(--neon-pink)', boxShadow: '0 0 25px rgba(255, 0, 92, 0.4)', maxWidth: '400px' }}>
            <h3 style={{ color: 'var(--neon-pink)', textAlign: 'center', marginBottom: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
              ⚠️ Gender Change Warning
            </h3>
            <p style={{ textAlign: 'center', marginBottom: '24px', lineHeight: '1.5', color: '#fff', fontSize: '0.95rem' }}>
              You are changing your profile gender to <strong style={{ color: userData.gender?.toLowerCase()?.trim() === 'male' ? 'var(--neon-pink)' : 'var(--neon-blue)' }}>{userData.gender?.toLowerCase()?.trim() === 'male' ? 'Female' : 'Male'}</strong>.
              <br/><br/>
              <span style={{ 
                color: 'var(--neon-pink)', 
                background: 'rgba(255, 0, 92, 0.1)', 
                border: '1px solid rgba(255, 0, 92, 0.3)', 
                borderRadius: '6px', 
                padding: '8px 12px',
                display: 'block',
                fontWeight: 'bold',
                fontSize: '0.85rem'
              }}>
                {userData.gender?.toLowerCase()?.trim() === 'male' 
                  ? '🔒 Female profiles require live face verification to start matching.' 
                  : '🔓 Male profiles are auto-verified on change.'}
              </span>
            </p>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button className="btn-neon" onClick={executeGenderChange} style={{ flex: 1, background: 'var(--neon-pink)', borderColor: 'var(--neon-pink)', color: '#fff' }}>
                Yes, Change
              </button>
              <button className="btn-glass" onClick={() => setShowGenderWarning(false)} style={{ flex: 1 }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
