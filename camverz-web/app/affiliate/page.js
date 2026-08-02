'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '../../lib/auth-context';
import { 
  getAffiliateMe, 
  applyAffiliate, 
  verifyAffiliateBio, 
  resetAffiliateVerification,
  verifyInstagramBio,
  verifyYoutubeBio,
  verifyOtherBio,
  updateAffiliateLinks,
  adminListAffiliates,
  adminApproveAffiliate,
  adminUpdateAffiliate,
  adminDeleteUser,
  adminWipeTrialData
} from '../../lib/api';
import { 
  Zap, 
  Users, 
  TrendingUp, 
  DollarSign, 
  Copy, 
  CheckCircle, 
  ArrowRight, 
  Link2, 
  Shield, 
  Clock, 
  Sparkles, 
  Check,
  AlertCircle,
  HelpCircle,
  TrendingDown,
  Calendar,
  ChevronDown
} from 'lucide-react';

export default function AffiliatePage() {
  const { user, loading: authLoading, setShowLogin } = useAuth();
  
  // App States
  const [affData, setAffData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [copiedWeb, setCopiedWeb] = useState(false);
  const [copiedApp, setCopiedApp] = useState(false);
  
  // Application Form States
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [upiId, setUpiId] = useState('');
  const [instagramUrl, setInstagramUrl] = useState('');
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [otherUrl, setOtherUrl] = useState('');
  const [confirmOwnership, setConfirmOwnership] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Verification States
  const [verifying, setVerifying] = useState(false);
  const [verifyingInstagram, setVerifyingInstagram] = useState(false);
  const [verifyingYoutube, setVerifyingYoutube] = useState(false);
  const [verifyingOther, setVerifyingOther] = useState(false);
  const [resetting, setResetting] = useState(false);

  // FAQ Accordion State
  const [openFaq, setOpenFaq] = useState(null);

  // Editing Links states
  const [isEditingLinks, setIsEditingLinks] = useState(false);
  const [editInstagramUrl, setEditInstagramUrl] = useState('');
  const [editYoutubeUrl, setEditYoutubeUrl] = useState('');
  const [editOtherUrl, setEditOtherUrl] = useState('');
  const [updatingLinks, setUpdatingLinks] = useState(false);
  const [editErrorMsg, setEditErrorMsg] = useState('');
  const [editSuccessMsg, setEditSuccessMsg] = useState('');

  // Admin Panel states
  const [adminList, setAdminList] = useState([]);
  const [loadingAdmin, setLoadingAdmin] = useState(false);
  const [updatingAdminId, setUpdatingAdminId] = useState('');
  const [activeAdminTab, setActiveAdminTab] = useState('creators'); // 'creators' or 'affiliates'
  const [searchTerm, setSearchTerm] = useState('');
  const [planFilter, setPlanFilter] = useState('all'); // 'all', 'paid', 'free'
  const [genderFilter, setGenderFilter] = useState('all'); // 'all', 'male', 'female'
  const [verificationFilter, setVerificationFilter] = useState('all'); // 'all', 'verified', 'unverified'

  // Fetch Affiliate Data
  const loadAffiliateData = async () => {
    if (!user) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      const data = await getAffiliateMe();
      setAffData(data);
    } catch (err) {
      console.error("Failed to load affiliate details:", err);
    } finally {
      setLoading(false);
    }
  };

  const loadAdminData = async () => {
    if (user?.email === 'mohitjain1619@gmail.com') {
      try {
        setLoadingAdmin(true);
        const list = await adminListAffiliates();
        setAdminList(list);
      } catch (err) {
        console.error("Failed to load admin list:", err);
      } finally {
        setLoadingAdmin(false);
      }
    }
  };

  useEffect(() => {
    loadAffiliateData();
    if (user && user.email === 'mohitjain1619@gmail.com') {
      loadAdminData();
    }
  }, [user]);

  // Copy to clipboard handler
  const handleCopyWeb = (text) => {
    navigator.clipboard.writeText(text);
    setCopiedWeb(true);
    setTimeout(() => setCopiedWeb(false), 2000);
  };

  const handleCopyApp = (text) => {
    navigator.clipboard.writeText(text);
    setCopiedApp(true);
    setTimeout(() => setCopiedApp(false), 2000);
  };

  // Submit Creator Application
  const handleSubmitApplication = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!name.trim() || !code.trim()) {
      setErrorMsg("Name and Preferred Code are required.");
      return;
    }
    if (!instagramUrl.trim() && !youtubeUrl.trim() && !otherUrl.trim()) {
      setErrorMsg("Please provide at least one platform link (Instagram, YouTube, or Other Platform).");
      return;
    }

    if (otherUrl.trim()) {
      const lowerOther = otherUrl.trim().toLowerCase();
      if (!lowerOther.includes("xhamster") && !lowerOther.includes("faphouse")) {
        setErrorMsg("Other platform URL must be a valid xHamster or Faphouse profile link.");
        return;
      }
    }

    if (!confirmOwnership) {
      setErrorMsg("You must confirm ownership of the social profiles.");
      return;
    }

    try {
      setSubmitting(true);
      const response = await applyAffiliate({
        name: name.trim(),
        preferredCode: code.trim().toUpperCase(),
        instagramUrl: instagramUrl.trim() || null,
        youtubeUrl: youtubeUrl.trim() || null,
        otherUrl: otherUrl.trim() || null,
        confirmOwnership
      });

      if (response.status === 'success') {
        setSuccessMsg(response.message);
        setName('');
        setCode('');
        setInstagramUrl('');
        setYoutubeUrl('');
        setOtherUrl('');
        setConfirmOwnership(false);
        await loadAffiliateData();
        
        // Scroll to form message
        document.getElementById('apply-section')?.scrollIntoView({ behavior: 'smooth' });
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.error || err.message || "Application submission failed. Please try another code.");
    } finally {
      setSubmitting(false);
    }
  };

  // Run Bio Verification
  const handleVerifyBio = async (simulate = false) => {
    setErrorMsg('');
    try {
      setVerifying(true);
      const response = await verifyAffiliateBio(simulate);
      if (response.status === 'success') {
        alert(response.message);
        await loadAffiliateData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.error || "Bio verification failed. Ensure the code is visible in your bio.");
    } finally {
      setVerifying(false);
    }
  };

  const handleVerifyInstagram = async () => {
    setErrorMsg('');
    try {
      setVerifyingInstagram(true);
      const response = await verifyInstagramBio();
      if (response.status === 'success') {
        alert(response.message);
        await loadAffiliateData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.error || err.message || "Instagram bio verification failed.");
    } finally {
      setVerifyingInstagram(false);
    }
  };

  const handleVerifyYoutube = async () => {
    setErrorMsg('');
    try {
      setVerifyingYoutube(true);
      const response = await verifyYoutubeBio();
      if (response.status === 'success') {
        alert(response.message);
        await loadAffiliateData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.error || err.message || "YouTube channel bio verification failed.");
    } finally {
      setVerifyingYoutube(false);
    }
  };

  const handleVerifyOther = async () => {
    setErrorMsg('');
    try {
      setVerifyingOther(true);
      const response = await verifyOtherBio();
      if (response.status === 'success') {
        alert(response.message);
        await loadAffiliateData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.error || err.message || "Other platform bio verification failed.");
    } finally {
      setVerifyingOther(false);
    }
  };

  // Reset Verification status
  const handleResetVerification = async () => {
    if (!window.confirm("Do you want to reset verification and start over?")) return;
    try {
      setResetting(true);
      const response = await resetAffiliateVerification();
      if (response.status === 'success') {
        await loadAffiliateData();
      }
    } catch (err) {
      console.error(err);
    } finally {
      setResetting(false);
    }
  };

  const handleStartEditing = () => {
    if (affData?.affiliate) {
      setEditInstagramUrl(affData.affiliate.instagram_url || '');
      setEditYoutubeUrl(affData.affiliate.youtube_url || '');
      setEditOtherUrl(affData.affiliate.other_url || '');
      setEditErrorMsg('');
      setEditSuccessMsg('');
      setIsEditingLinks(true);
    }
  };

  const handleUpdateLinksSubmit = async (e) => {
    e.preventDefault();
    setEditErrorMsg('');
    setEditSuccessMsg('');

    if (!editInstagramUrl.trim() && !editYoutubeUrl.trim() && !editOtherUrl.trim()) {
      setEditErrorMsg("Please provide at least one platform link.");
      return;
    }

    if (editOtherUrl.trim()) {
      const lower = editOtherUrl.trim().toLowerCase();
      if (!lower.includes("xhamster") && !lower.includes("faphouse")) {
        setEditErrorMsg("Other platform URL must be a valid xHamster or Faphouse profile link.");
        return;
      }
    }

    try {
      setUpdatingLinks(true);
      const res = await updateAffiliateLinks({
        instagramUrl: editInstagramUrl.trim() || null,
        youtubeUrl: editYoutubeUrl.trim() || null,
        otherUrl: editOtherUrl.trim() || null
      });

      if (res.status === 'success') {
        setEditSuccessMsg(res.message);
        await loadAffiliateData();
        setTimeout(() => {
          setIsEditingLinks(false);
        }, 1500);
      }
    } catch (err) {
      setEditErrorMsg(err.response?.data?.error || err.message || "Failed to update links.");
    } finally {
      setUpdatingLinks(false);
    }
  };

  const handleAdminApprove = async (id) => {
    if (!window.confirm("Are you sure you want to approve this creator?")) return;
    try {
      setUpdatingAdminId(id);
      const res = await adminApproveAffiliate(id);
      if (res.status === 'success') {
        alert(res.message);
        await loadAdminData();
        await loadAffiliateData(); // Refresh current page state too
      }
    } catch (err) {
      alert(err.response?.data?.error || err.message || "Failed to approve creator.");
    } finally {
      setUpdatingAdminId('');
    }
  };

  const handleAdminDeleteUser = async (userId, userEmail) => {
    if (!window.confirm(`WARNING: Are you sure you want to delete ${userEmail}'s account? This will permanently erase their profile, social configurations, referrals, and all database data using ON DELETE CASCADE.`)) return;
    try {
      setUpdatingAdminId(userId);
      const res = await adminDeleteUser(userId);
      if (res.status === 'success') {
        alert(res.message);
        await loadAdminData();
      }
    } catch (err) {
      alert(err.response?.data?.error || err.message || "Failed to delete user account.");
    } finally {
      setUpdatingAdminId('');
    }
  };

  const handleAdminWipeTrialData = async () => {
    if (!window.confirm("DANGER WIPE: Are you sure you want to delete ALL database trial accounts? This will keep ONLY mohitjain1619@gmail.com and delete everyone else (cascade wipes clicks, signups, logs).")) return;
    try {
      setLoadingAdmin(true);
      const res = await adminWipeTrialData();
      if (res.status === 'success') {
        alert(res.message);
        await loadAdminData();
      }
    } catch (err) {
      alert(err.response?.data?.error || err.message || "Failed to wipe database.");
    } finally {
      setLoadingAdmin(false);
    }
  };

  const renderAdminPanel = () => {
    // Filter adminList into Creators (have social links) and Affiliates (no social links)
    const creators = adminList.filter(c => c.instagram_url || c.youtube_url || c.other_url);
    const affiliates = adminList.filter(c => !c.instagram_url && !c.youtube_url && !c.other_url);

    const activeList = activeAdminTab === 'creators' ? creators : affiliates;

    return (
      <div className="glass-card" style={{ padding: '30px', marginTop: '40px', border: '1px solid var(--neon-purple)', width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '24px' }}>
          <div>
            <h3 style={{ fontSize: '1.4rem', margin: '0 0 8px 0', color: 'var(--neon-purple)', display: 'flex', gap: '8px', alignItems: 'center' }}>
              <span>🔐 Admin Panel (Applications Control)</span>
            </h3>
            <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.85rem' }}>
              Review applications, delete dummy user accounts, and wipe database trials.
            </p>
          </div>
          <button 
            className="btn-glass" 
            onClick={handleAdminWipeTrialData}
            style={{ 
              borderColor: '#ef4444', 
              color: '#ef4444', 
              padding: '8px 16px', 
              fontSize: '0.8rem',
              background: 'rgba(239, 68, 68, 0.05)'
            }}
          >
            🔥 Wipe Trial Data (Admin Only)
          </button>
        </div>

        {/* Tab Selection */}
        <div style={{ display: 'flex', gap: '12px', borderBottom: '1px solid var(--glass-border)', paddingBottom: '12px', marginBottom: '20px' }}>
          <button 
            onClick={() => setActiveAdminTab('creators')}
            style={{
              background: activeAdminTab === 'creators' ? 'rgba(168, 85, 247, 0.15)' : 'transparent',
              color: activeAdminTab === 'creators' ? 'var(--neon-purple)' : 'var(--text-secondary)',
              border: 'none',
              padding: '8px 16px',
              borderRadius: '6px',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 0.2s ease-in-out'
            }}
          >
            🎬 Creators ({creators.length})
          </button>
          <button 
            onClick={() => setActiveAdminTab('affiliates')}
            style={{
              background: activeAdminTab === 'affiliates' ? 'rgba(168, 85, 247, 0.15)' : 'transparent',
              color: activeAdminTab === 'affiliates' ? 'var(--neon-purple)' : 'var(--text-secondary)',
              border: 'none',
              padding: '8px 16px',
              borderRadius: '6px',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 0.2s ease-in-out'
            }}
          >
            🤝 Affiliates ({affiliates.length})
          </button>
        </div>

        {loadingAdmin ? (
          <p style={{ color: 'var(--neon-cyan)', fontSize: '0.9rem' }}>Loading records list...</p>
        ) : activeList.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            No {activeAdminTab} found in the database.
          </p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem', minWidth: '800px' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--glass-border)', color: 'var(--text-muted)' }}>
                  <th style={{ textAlign: 'left', padding: '10px' }}>NAME</th>
                  <th style={{ textAlign: 'left', padding: '10px' }}>EMAIL</th>
                  <th style={{ textAlign: 'left', padding: '10px' }}>REF CODE</th>
                  {activeAdminTab === 'creators' && <th style={{ textAlign: 'left', padding: '10px' }}>PROFILES & VERIFIED STATUS</th>}
                  <th style={{ textAlign: 'center', padding: '10px' }}>STATUS</th>
                  <th style={{ textAlign: 'center', padding: '10px' }}>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {activeList.map((c) => (
                  <tr key={c.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <td style={{ padding: '12px 10px', fontWeight: 600 }}>{c.name}</td>
                    <td style={{ padding: '12px 10px', color: 'var(--text-secondary)' }}>{c.email}</td>
                    <td style={{ padding: '12px 10px', fontFamily: 'monospace', color: 'var(--neon-cyan)' }}>{c.code}</td>
                    {activeAdminTab === 'creators' && (
                      <td style={{ padding: '12px 10px' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                          {c.instagram_url && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <span style={{ fontSize: '0.9rem' }}>📸</span>
                              <a href={c.instagram_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline', maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Instagram</a>
                              <span style={{ fontSize: '0.7rem', color: c.instagram_verified ? 'var(--neon-green)' : '#f59e0b', fontWeight: 600 }}>
                                ({c.instagram_verified ? 'Verified' : 'Pending'})
                              </span>
                            </div>
                          )}
                          {c.youtube_url && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <span style={{ fontSize: '0.9rem' }}>🎬</span>
                              <a href={c.youtube_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline', maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>YouTube</a>
                              <span style={{ fontSize: '0.7rem', color: c.youtube_verified ? 'var(--neon-green)' : '#f59e0b', fontWeight: 600 }}>
                                ({c.youtube_verified ? 'Verified' : 'Pending'})
                              </span>
                            </div>
                          )}
                          {c.other_url && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <span style={{ fontSize: '0.9rem' }}>🌐</span>
                              <a href={c.other_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline', maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Other Profile</a>
                              <span style={{ fontSize: '0.7rem', color: c.other_verified ? 'var(--neon-green)' : '#f59e0b', fontWeight: 600 }}>
                                ({c.other_verified ? 'Verified' : 'Pending'})
                              </span>
                            </div>
                          )}
                        </div>
                      </td>
                    )}
                    <td style={{ padding: '12px 10px', textAlign: 'center' }}>
                      <span style={{ 
                        display: 'inline-block',
                        padding: '2px 8px', 
                        borderRadius: '4px', 
                        fontSize: '0.7rem', 
                        fontWeight: 600, 
                        background: c.status === 'approved' ? 'rgba(0, 230, 118, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                        color: c.status === 'approved' ? 'var(--neon-green)' : '#f59e0b'
                      }}>
                        {c.status.toUpperCase()}
                      </span>
                    </td>
                    <td style={{ padding: '12px 10px', textAlign: 'center' }}>
                      <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', alignItems: 'center' }}>
                        {c.status === 'pending' && (
                          <button 
                            className="btn-neon" 
                            style={{ padding: '6px 12px', fontSize: '0.75rem' }} 
                            onClick={() => handleAdminApprove(c.id)}
                            disabled={updatingAdminId === c.id}
                          >
                            {updatingAdminId === c.id ? "Approving..." : "✓ Approve"}
                          </button>
                        )}
                        <button
                          className="btn-glass"
                          style={{ 
                            padding: '6px 12px', 
                            fontSize: '0.75rem', 
                            borderColor: '#ef4444', 
                            color: '#ef4444',
                            background: 'rgba(239, 68, 68, 0.05)'
                          }}
                          onClick={() => handleAdminDeleteUser(c.user_id, c.email)}
                          disabled={updatingAdminId === c.user_id}
                        >
                          🗑️ Delete Account
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  };

  const siteUrl = typeof window !== 'undefined' ? window.location.origin : 'https://camverz-nine.vercel.app';
  const referralLink = affData?.affiliate ? `${siteUrl}?ref=${affData.affiliate.code}` : '';
  const appReferralLink = affData?.affiliate ? `https://play.google.com/store/apps/details?id=com.mohitt.camverz&referrer=${affData.affiliate.code}` : '';

  const faqs = [
    {
      q: "How does the Camverz Creator Partnership work?",
      a: "Simple: Join the program, claim your unique code, and share your referral link with your audience. When someone visits our platform via your link and signs up, they are linked to you forever. You earn a 25% lifetime commission on all call package purchases they make."
    },
    {
      q: "What does 'lifetime commission' mean?",
      a: "It means you continue to earn commission on renewals, coins, packages, and any future premium features purchased by your referred users — for life, with no expiry date."
    },
    {
      q: "How is tracking calculated?",
      a: "We use a 30-day cookie window. If a user clicks your link and signs up within 30 days, they are permanently associated with your creator account. Last click attribution applies."
    },
    {
      q: "When do I get paid?",
      a: "Payouts are processed monthly. Once your pending balance reaches our minimum payout threshold of ₹8,000, we transfer the earnings directly to your UPI ID or bank account."
    }
  ];

  if (authLoading || (loading && user)) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh', color: 'var(--neon-cyan)' }}>
        <div style={{ textAlign: 'center' }}>
          <div className="skeleton" style={{ width: '60px', height: '60px', borderRadius: '50%', margin: '0 auto 20px' }} />
          <p style={{ fontFamily: 'var(--font-display)', fontWeight: 600 }}>Loading Creator Dashboard...</p>
        </div>
      </div>
    );
  }

  // Creator profile status checks
  const isApprovedCreator = user && affData?.has_affiliate && affData.affiliate?.status === 'approved';
  const isPendingCreator = user && affData?.has_affiliate && affData.affiliate?.status === 'pending';
  const isNewCreator = user && (!affData || !affData.has_affiliate);
  const isGuest = !user;

  // ═══════════════════════════════════════════════════════════
  // 1. APPROVED CREATOR DASHBOARD VIEW (Direct Business Panel)
  // ═══════════════════════════════════════════════════════════
  if (isApprovedCreator) {
    const { affiliate, stats, recent_sales, payouts, click_chart } = affData;

    const filteredReferredUsers = (affData.referred_users || []).filter(su => {
      const matchesSearch = (su.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                            (su.email || '').toLowerCase().includes(searchTerm.toLowerCase());
      const matchesPlan = planFilter === 'all' || (su.plan || '').toLowerCase() === planFilter.toLowerCase();
      
      const g = (su.gender || '').toLowerCase();
      const isFemale = g.includes("female") || g.includes("girl") || g.includes("woman");
      const isMale = g.includes("male") || g.includes("boy") || g.includes("man");
      const matchesGender = genderFilter === 'all' ||
                            (genderFilter === 'female' && isFemale) ||
                            (genderFilter === 'male' && isMale);
                            
      const matchesVerification = verificationFilter === 'all' ||
                                  (verificationFilter === 'verified' && su.verified) ||
                                  (verificationFilter === 'unverified' && !su.verified);
                                  
      return matchesSearch && matchesPlan && matchesGender && matchesVerification;
    });

    return (
      <div className="section" style={{ paddingTop: '90px', paddingBottom: '60px' }}>
        {/* Welcome Banner */}
        <div className="glass-card" style={{ padding: '30px', marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '20px' }}>
          <div>
            <h2 style={{ marginBottom: '6px' }}>Welcome, {affiliate.name}!</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', marginBottom: '12px' }}>
              Code: <strong style={{ color: 'var(--neon-green)' }}>{affiliate.code}</strong> | Status: <span style={{ color: 'var(--neon-green)', fontWeight: 600 }}>Active</span>
            </p>
            <button 
              className="btn-glass" 
              onClick={handleStartEditing}
              style={{ padding: '8px 16px', fontSize: '0.8rem' }}
            >
              ✏️ Edit Profile Links
            </button>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', minWidth: '320px' }}>
            {/* Website Referral Link */}
            <div style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid var(--glass-border)', padding: '12px 20px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '12px', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 600 }}>WEBSITE REFERRAL LINK</span>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-primary)', fontFamily: 'monospace' }}>{referralLink}</span>
              </div>
              <button 
                onClick={() => handleCopyWeb(referralLink)} 
                style={{ 
                  width: '36px', 
                  height: '36px', 
                  borderRadius: '8px', 
                  background: copiedWeb ? 'rgba(0, 230, 118, 0.15)' : 'var(--glass-bg)', 
                  border: `1px solid ${copiedWeb ? 'var(--neon-green)' : 'var(--glass-border)'}`,
                  display: 'flex', 
                  justifyContent: 'center', 
                  alignItems: 'center',
                  color: copiedWeb ? 'var(--neon-green)' : 'var(--neon-cyan)',
                  cursor: 'pointer',
                  transition: 'all 200ms ease',
                  flexShrink: 0
                }}
              >
                {copiedWeb ? <Check size={16} /> : <Copy size={16} />}
              </button>
            </div>

            {/* Play Store Direct Link */}
            <div style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid var(--glass-border)', padding: '12px 20px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '12px', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 600 }}>PLAY STORE REFERRAL LINK (DIRECT)</span>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-primary)', fontFamily: 'monospace' }}>{appReferralLink}</span>
              </div>
              <button 
                onClick={() => handleCopyApp(appReferralLink)} 
                style={{ 
                  width: '36px', 
                  height: '36px', 
                  borderRadius: '8px', 
                  background: copiedApp ? 'rgba(0, 230, 118, 0.15)' : 'var(--glass-bg)', 
                  border: `1px solid ${copiedApp ? 'var(--neon-green)' : 'var(--glass-border)'}`,
                  display: 'flex', 
                  justifyContent: 'center', 
                  alignItems: 'center',
                  color: copiedApp ? 'var(--neon-green)' : 'var(--neon-cyan)',
                  cursor: 'pointer',
                  transition: 'all 200ms ease',
                  flexShrink: 0
                }}
              >
                {copiedApp ? <Check size={16} /> : <Copy size={16} />}
              </button>
            </div>
          </div>
        </div>

        {/* Edit Links Form */}
        {isEditingLinks && (
          <div className="glass-card" style={{ padding: '30px', marginBottom: '32px', border: '1px solid var(--neon-cyan)' }}>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '8px' }}>Update Profile Links</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '20px', fontSize: '0.85rem' }}>
              Modify your social profile URLs below. Note: if you change any URL, you must re-verify that profile using a new verification code.
            </p>

            {editErrorMsg && (
              <div style={{ background: 'rgba(255, 0, 110, 0.1)', border: '1px solid rgba(255, 0, 110, 0.3)', padding: '10px 14px', borderRadius: '8px', color: 'var(--neon-pink)', marginBottom: '16px', fontSize: '0.85rem' }}>
                {editErrorMsg}
              </div>
            )}
            {editSuccessMsg && (
              <div style={{ background: 'rgba(0, 230, 118, 0.1)', border: '1px solid rgba(0, 230, 118, 0.3)', padding: '10px 14px', borderRadius: '8px', color: 'var(--neon-green)', marginBottom: '16px', fontSize: '0.85rem' }}>
                {editSuccessMsg}
              </div>
            )}

            <form onSubmit={handleUpdateLinksSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>INSTAGRAM PROFILE URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="https://instagram.com/username" 
                    value={editInstagramUrl} 
                    onChange={(e) => setEditInstagramUrl(e.target.value)} 
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>YOUTUBE CHANNEL URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="https://youtube.com/@channel" 
                    value={editYoutubeUrl} 
                    onChange={(e) => setEditYoutubeUrl(e.target.value)} 
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>OTHER PLATFORM URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="https://example.com/profile" 
                    value={editOtherUrl} 
                    onChange={(e) => setEditOtherUrl(e.target.value)} 
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
                <button type="submit" className="btn-neon" style={{ padding: '10px 24px', fontSize: '0.85rem' }} disabled={updatingLinks}>
                  {updatingLinks ? "Saving..." : "Save & Verify"}
                </button>
                <button type="button" className="btn-glass" onClick={() => setIsEditingLinks(false)} style={{ padding: '10px 24px', fontSize: '0.85rem' }}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Verification Grid for Unverified Updated URLs */}
        {(() => {
          const hasUnverified = 
            (affiliate.instagram_url && !affiliate.instagram_verified) ||
            (affiliate.youtube_url && !affiliate.youtube_verified) ||
            (affiliate.other_url && !affiliate.other_verified);

          if (!hasUnverified) return null;

          return (
            <div className="glass-card" style={{ padding: '30px', marginBottom: '32px', border: '1px solid rgba(245, 158, 11, 0.3)' }}>
              <h3 style={{ fontSize: '1.3rem', marginBottom: '8px', color: '#f59e0b' }}>⚠️ Verification Action Required</h3>
              <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '0.85rem' }}>
                You have updated profile links that are not yet verified. Please verify them below to complete validation.
              </p>

              {errorMsg && (
                <div style={{ background: 'rgba(255, 0, 110, 0.1)', border: '1px solid rgba(255, 0, 110, 0.3)', padding: '12px 16px', borderRadius: '8px', color: 'var(--neon-pink)', marginBottom: '20px', fontSize: '0.9rem' }}>
                  {errorMsg}
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
                {affiliate.instagram_url && !affiliate.instagram_verified && (
                  <div className="glass-card" style={{ padding: '20px' }}>
                    <h4 style={{ margin: '0 0 10px 0' }}>📸 Instagram</h4>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '12px', wordBreak: 'break-all' }}>{affiliate.instagram_url}</p>
                    <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px', border: '1px solid var(--glass-border)', marginBottom: '12px' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Paste this code in your Instagram bio:</span>
                      <div style={{ background: 'rgba(255,0,110,0.08)', padding: '8px', borderRadius: '6px', margin: '6px 0', fontFamily: 'monospace', fontWeight: 700, color: 'var(--neon-pink)', textAlign: 'center', cursor: 'pointer' }}
                        onClick={() => { navigator.clipboard.writeText(affiliate.instagram_bio_code); alert('Instagram code copied!'); }}
                      >
                        {affiliate.instagram_bio_code} 📋
                      </div>
                    </div>
                    <button className="btn-neon" style={{ width: '100%', padding: '10px' }} onClick={handleVerifyInstagram} disabled={verifyingInstagram}>
                      {verifyingInstagram ? "Verifying..." : "Verify Instagram"}
                    </button>
                  </div>
                )}

                {affiliate.youtube_url && !affiliate.youtube_verified && (
                  <div className="glass-card" style={{ padding: '20px' }}>
                    <h4 style={{ margin: '0 0 10px 0' }}>🎬 YouTube</h4>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '12px', wordBreak: 'break-all' }}>{affiliate.youtube_url}</p>
                    <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px', border: '1px solid var(--glass-border)', marginBottom: '12px' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Paste this code in your YouTube description:</span>
                      <div style={{ background: 'rgba(255,0,110,0.08)', padding: '8px', borderRadius: '6px', margin: '6px 0', fontFamily: 'monospace', fontWeight: 700, color: 'var(--neon-pink)', textAlign: 'center', cursor: 'pointer' }}
                        onClick={() => { navigator.clipboard.writeText(affiliate.youtube_bio_code); alert('YouTube code copied!'); }}
                      >
                        {affiliate.youtube_bio_code} 📋
                      </div>
                    </div>
                    <button className="btn-neon" style={{ width: '100%', padding: '10px' }} onClick={handleVerifyYoutube} disabled={verifyingYoutube}>
                      {verifyingYoutube ? "Verifying..." : "Verify YouTube"}
                    </button>
                  </div>
                )}

                {affiliate.other_url && !affiliate.other_verified && (
                  <div className="glass-card" style={{ padding: '20px' }}>
                    <h4 style={{ margin: '0 0 10px 0' }}>🌐 Other Platform</h4>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '12px', wordBreak: 'break-all' }}>{affiliate.other_url}</p>
                    <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px', border: '1px solid var(--glass-border)', marginBottom: '12px' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {(() => {
                          const url = affiliate.other_url.toLowerCase();
                          if (url.includes("xhamster")) return "Switch browser to Desktop Mode, go to xHamster profile's About, and paste this code:";
                          if (url.includes("faphouse")) return "Switch browser to Desktop Mode, go to Faphouse profile's Bio, and paste this code:";
                          return "Paste this code in your profile description/bio:";
                        })()}
                      </span>
                      <div style={{ background: 'rgba(255,0,110,0.08)', padding: '8px', borderRadius: '6px', margin: '6px 0', fontFamily: 'monospace', fontWeight: 700, color: 'var(--neon-pink)', textAlign: 'center', cursor: 'pointer' }}
                        onClick={() => { navigator.clipboard.writeText(affiliate.other_bio_code); alert('Other platform code copied!'); }}
                      >
                        {affiliate.other_bio_code} 📋
                      </div>
                    </div>
                    <button className="btn-neon" style={{ width: '100%', padding: '10px' }} onClick={handleVerifyOther} disabled={verifyingOther}>
                      {verifyingOther ? "Verifying..." : "Verify Profile"}
                    </button>
                  </div>
                )}
              </div>
            </div>
          );
        })()}

        {/* Metrics Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '20px', marginBottom: '32px' }}>
          <div className="glass-card" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>CLICKS</span>
              <Link2 size={16} color="var(--neon-cyan)" />
            </div>
            <h2 style={{ fontSize: '2rem', fontWeight: 800 }}>{stats.total_clicks}</h2>
          </div>

          <div className="glass-card" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>SIGNUPS</span>
              <Users size={16} color="var(--neon-purple)" />
            </div>
            <h2 style={{ fontSize: '2rem', fontWeight: 800 }}>{stats.total_signups}</h2>
          </div>

          <div className="glass-card" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>CONVERSIONS</span>
              <Zap size={16} color="var(--neon-pink)" />
            </div>
            <h2 style={{ fontSize: '2rem', fontWeight: 800 }}>{stats.total_sales}</h2>
          </div>

          <div className="glass-card" style={{ padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>CONV. RATE</span>
              <TrendingUp size={16} color="var(--neon-green)" />
            </div>
            <h2 style={{ fontSize: '2rem', fontWeight: 800 }}>{stats.conversion_rate}%</h2>
          </div>

          <div className="glass-card" style={{ padding: '24px', border: '1px solid rgba(0, 230, 118, 0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>NET EARNINGS</span>
              <DollarSign size={16} color="var(--neon-green)" />
            </div>
            <h2 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--neon-green)' }}>₹{stats.net_earnings}</h2>
          </div>

          <div className="glass-card" style={{ padding: '24px', border: '1px solid rgba(0, 229, 255, 0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>UNPAID BALANCE</span>
              <DollarSign size={16} color="var(--neon-cyan)" />
            </div>
            <h2 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--neon-cyan)' }}>₹{stats.pending_payout}</h2>
            <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Min payout: ₹{affiliate.min_payout}</span>
          </div>
        </div>

        {/* Clicks Trend Visualizer (30 Day) */}
        <div className="glass-card" style={{ padding: '30px', marginBottom: '32px' }}>
          <h3 style={{ marginBottom: '20px', display: 'flex', gap: '8px', alignItems: 'center' }}>
            <TrendingUp size={20} color="var(--neon-cyan)" />
            <span>Clicks Trend (Last 30 Days)</span>
          </h3>
          
          {click_chart && click_chart.length > 0 ? (
            <div style={{ display: 'flex', alignItems: 'flex-end', height: '150px', gap: '6px', paddingTop: '20px', borderBottom: '1px solid var(--glass-border)', overflowX: 'auto', paddingBottom: '10px' }}>
              {click_chart.map((c, i) => {
                const maxClicks = Math.max(...click_chart.map(x => x.clicks), 1);
                const heightPct = Math.max(10, (c.clicks / maxClicks) * 100);
                
                return (
                  <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '16px' }}>
                    <span style={{ fontSize: '0.65rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>{c.clicks}</span>
                    <div 
                      title={`${c.day}: ${c.clicks} clicks`}
                      style={{ 
                        width: '100%', 
                        height: `${heightPct}px`, 
                        background: 'var(--gradient-neon)',
                        borderRadius: '4px 4px 0 0',
                        transition: 'height 0.3s ease'
                      }} 
                    />
                    <span style={{ fontSize: '0.55rem', color: 'var(--text-muted)', marginTop: '6px', transform: 'rotate(-45deg)', whiteSpace: 'nowrap', height: '24px' }}>
                      {c.day.split('-')[2]}
                    </span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div style={{ padding: '40px 0', textCenter: 'center', color: 'var(--text-secondary)', fontSize: '0.95rem' }} className="text-center">
              No visitor clicks recorded yet in the last 30 days. Share your link to start tracking!
            </div>
          )}
        </div>

        {/* History Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '32px' }}>
          
          {/* Recent Sales Table */}
          <div className="glass-card" style={{ padding: '24px', overflowX: 'auto' }}>
            <h3 style={{ marginBottom: '16px', display: 'flex', gap: '8px', alignItems: 'center' }}>
              <Zap size={18} color="var(--neon-pink)" />
              <span>Recent Conversions</span>
            </h3>
            {recent_sales && recent_sales.length > 0 ? (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--glass-border)', color: 'var(--text-muted)' }}>
                    <th style={{ textAlign: 'left', padding: '12px 8px' }}>PLAN</th>
                    <th style={{ textAlign: 'left', padding: '12px 8px' }}>DATE</th>
                    <th style={{ textAlign: 'right', padding: '12px 8px' }}>AMOUNT</th>
                    <th style={{ textAlign: 'right', padding: '12px 8px' }}>COMMISSION</th>
                    <th style={{ textAlign: 'center', padding: '12px 8px' }}>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {recent_sales.map((sale) => (
                    <tr key={sale.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                      <td style={{ padding: '12px 8px', fontWeight: 600 }}>{sale.plan}</td>
                      <td style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>{sale.date.split(' ')[0]}</td>
                      <td style={{ padding: '12px 8px', textAlign: 'right', color: 'var(--text-secondary)' }}>₹{sale.amount}</td>
                      <td style={{ padding: '12px 8px', textAlign: 'right', fontWeight: 600, color: sale.status === 'refunded' ? 'var(--neon-pink)' : 'var(--neon-green)' }}>
                        {sale.status === 'refunded' ? `-₹${sale.commission_clawback}` : `₹${sale.commission}`}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        <span style={{ 
                          display: 'inline-block',
                          padding: '2px 8px', 
                          borderRadius: '4px', 
                          fontSize: '0.7rem', 
                          fontWeight: 600, 
                          background: sale.status === 'refunded' ? 'rgba(255, 0, 110, 0.15)' : 'rgba(0, 230, 118, 0.15)',
                          color: sale.status === 'refunded' ? 'var(--neon-pink)' : 'var(--neon-green)'
                        }}>
                          {sale.status.toUpperCase()}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ color: 'var(--text-secondary)', padding: '24px 0' }} className="text-center">
                No conversions recorded yet.
              </div>
            )}
          </div>

          {/* Payout History Table */}
          <div className="glass-card" style={{ padding: '24px', overflowX: 'auto' }}>
            <h3 style={{ marginBottom: '16px', display: 'flex', gap: '8px', alignItems: 'center' }}>
              <CheckCircle size={18} color="var(--neon-green)" />
              <span>Payout History</span>
            </h3>
            {payouts && payouts.length > 0 ? (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--glass-border)', color: 'var(--text-muted)' }}>
                    <th style={{ textAlign: 'left', padding: '12px 8px' }}>DATE</th>
                    <th style={{ textAlign: 'left', padding: '12px 8px' }}>METHOD</th>
                    <th style={{ textAlign: 'left', padding: '12px 8px' }}>REF</th>
                    <th style={{ textAlign: 'right', padding: '12px 8px' }}>AMOUNT</th>
                    <th style={{ textAlign: 'center', padding: '12px 8px' }}>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {payouts.map((payout) => (
                    <tr key={payout.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                      <td style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>{payout.date.split(' ')[0]}</td>
                      <td style={{ padding: '12px 8px', textTransform: 'uppercase' }}>{payout.method}</td>
                      <td style={{ padding: '12px 8px', fontFamily: 'monospace', color: 'var(--text-secondary)' }}>
                        {payout.transaction_ref ? payout.transaction_ref.substring(0, 10) + '...' : 'N/A'}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'right', fontWeight: 600 }}>₹{payout.amount}</td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        <span style={{ 
                          display: 'inline-block',
                          padding: '2px 8px', 
                          borderRadius: '4px', 
                          fontSize: '0.7rem', 
                          fontWeight: 600, 
                          background: 'rgba(0, 230, 118, 0.15)',
                          color: 'var(--neon-green)'
                        }}>
                          COMPLETED
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ color: 'var(--text-secondary)', padding: '24px 0' }} className="text-center">
                No payouts completed yet.
              </div>
            )}
          </div>
        </div>

        {/* Referred Signups & Activity Section */}
        <div style={{ marginTop: '40px' }}>
          <h3 style={{ marginBottom: '20px', display: 'flex', gap: '8px', alignItems: 'center' }}>
            <Users size={20} color="var(--neon-purple)" />
            <span>Referred Signups & Activity</span>
          </h3>

          {/* Referred User Analytics Summary Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px', marginBottom: '32px' }}>
            <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>GENDER RATIO (BOYS / GIRLS)</span>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px' }}>
                <h2 style={{ fontSize: '1.8rem', fontWeight: 800, margin: 0, color: 'var(--neon-cyan)' }}>
                  {affData.analytics_summary?.total_boys || 0}
                </h2>
                <span style={{ color: 'var(--text-muted)' }}>boys</span>
                <span style={{ color: 'var(--glass-border)' }}>/</span>
                <h2 style={{ fontSize: '1.8rem', fontWeight: 800, margin: 0, color: 'var(--neon-pink)' }}>
                  {affData.analytics_summary?.total_girls || 0}
                </h2>
                <span style={{ color: 'var(--text-muted)' }}>girls</span>
              </div>
            </div>

            <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '8px', border: '1px solid rgba(0, 230, 118, 0.15)' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>VERIFIED GIRLS PROFILE COUNT</span>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                <h2 style={{ fontSize: '1.8rem', fontWeight: 800, margin: 0, color: 'var(--neon-green)' }}>
                  {affData.analytics_summary?.verified_girls || 0}
                </h2>
                <span style={{ fontSize: '0.8rem', color: 'var(--neon-green)', fontWeight: 600 }}>verified 👑</span>
              </div>
            </div>

            <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>AVG TALK TIME (LAST 7 DAYS)</span>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                <h2 style={{ fontSize: '1.8rem', fontWeight: 800, margin: 0, color: 'var(--neon-cyan)' }}>
                  {affData.analytics_summary?.average_7d_talktime_mins || 0}
                </h2>
                <span style={{ color: 'var(--text-muted)' }}>minutes / user</span>
              </div>
            </div>
          </div>

          {/* Referred Users Table List */}
          <div className="glass-card" style={{ padding: '24px', overflowX: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
              <h4 style={{ margin: 0, color: '#fff' }}>Referred Users Details</h4>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                Showing {filteredReferredUsers.length} of {(affData.referred_users || []).length} signups
              </span>
            </div>

            {/* Search and Filters */}
            <div style={{ 
              display: 'flex', 
              flexWrap: 'wrap', 
              gap: '12px', 
              marginBottom: '20px', 
              paddingBottom: '16px', 
              borderBottom: '1px solid var(--glass-border)' 
            }}>
              {/* Search Bar */}
              <div style={{ flex: '1 1 200px' }}>
                <input 
                  type="text" 
                  placeholder="🔍 Search referred user by name or email..." 
                  value={searchTerm} 
                  onChange={(e) => setSearchTerm(e.target.value)} 
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.04)',
                    border: '1px solid var(--glass-border)',
                    color: '#fff',
                    fontSize: '0.82rem',
                    outline: 'none',
                    transition: 'border-color 0.2s'
                  }}
                />
              </div>

              {/* Plan Filter */}
              <div style={{ minWidth: '120px' }}>
                <select 
                  value={planFilter} 
                  onChange={(e) => setPlanFilter(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.04)',
                    border: '1px solid var(--glass-border)',
                    color: '#fff',
                    fontSize: '0.82rem',
                    outline: 'none',
                    cursor: 'pointer'
                  }}
                >
                  <option value="all" style={{ background: '#1c103a' }}>💳 Plan: All</option>
                  <option value="paid" style={{ background: '#1c103a' }}>💳 Paid</option>
                  <option value="free" style={{ background: '#1c103a' }}>💳 Free</option>
                </select>
              </div>

              {/* Gender Filter */}
              <div style={{ minWidth: '130px' }}>
                <select 
                  value={genderFilter} 
                  onChange={(e) => setGenderFilter(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.04)',
                    border: '1px solid var(--glass-border)',
                    color: '#fff',
                    fontSize: '0.82rem',
                    outline: 'none',
                    cursor: 'pointer'
                  }}
                >
                  <option value="all" style={{ background: '#1c103a' }}>⚥ Gender: All</option>
                  <option value="male" style={{ background: '#1c103a' }}>♂️ Male</option>
                  <option value="female" style={{ background: '#1c103a' }}>♀️ Female</option>
                </select>
              </div>

              {/* Verification Filter */}
              <div style={{ minWidth: '150px' }}>
                <select 
                  value={verificationFilter} 
                  onChange={(e) => setVerificationFilter(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    background: 'rgba(255,255,255,0.04)',
                    border: '1px solid var(--glass-border)',
                    color: '#fff',
                    fontSize: '0.82rem',
                    outline: 'none',
                    cursor: 'pointer'
                  }}
                >
                  <option value="all" style={{ background: '#1c103a' }}>👑 Verification: All</option>
                  <option value="verified" style={{ background: '#1c103a' }}>👑 Verified</option>
                  <option value="unverified" style={{ background: '#1c103a' }}>👑 Unverified</option>
                </select>
              </div>
            </div>

            {filteredReferredUsers.length > 0 ? (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem', minWidth: '700px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--glass-border)', color: 'var(--text-muted)' }}>
                    <th style={{ textAlign: 'left', padding: '12px 8px' }}>USER</th>
                    <th style={{ textAlign: 'center', padding: '12px 8px' }}>GENDER</th>
                    <th style={{ textAlign: 'center', padding: '12px 8px' }}>PROFILE VERIFICATION</th>
                    <th style={{ textAlign: 'center', padding: '12px 8px' }}>PLAN STATUS</th>
                    <th style={{ textAlign: 'right', padding: '12px 8px' }}>7D TALK TIME</th>
                    <th style={{ textAlign: 'center', padding: '12px 8px' }}>ACCOUNT STATUS</th>
                    <th style={{ textAlign: 'right', padding: '12px 8px' }}>DATE JOINED</th>
                    {user?.email === 'mohitjain1619@gmail.com' && <th style={{ textAlign: 'center', padding: '12px 8px' }}>ACTIONS</th>}
                  </tr>
                </thead>
                <tbody>
                  {filteredReferredUsers.map((su, idx) => (
                    <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                      <td style={{ padding: '12px 8px' }}>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span style={{ fontWeight: 600, color: '#fff' }}>{su.name}</span>
                          <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{su.email}</span>
                        </div>
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        {(() => {
                          const g = su.gender.toLowerCase();
                          if (g.includes("female") || g.includes("girl")) return <span style={{ color: 'var(--neon-pink)' }}>♀️ Female</span>;
                          if (g.includes("male") || g.includes("boy")) return <span style={{ color: 'var(--neon-cyan)' }}>♂️ Male</span>;
                          return <span style={{ color: 'var(--text-muted)' }}>{su.gender}</span>;
                        })()}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        {su.verified ? (
                          <span style={{ color: 'var(--neon-green)', fontWeight: 600, fontSize: '0.75rem', border: '1px solid rgba(0, 230, 118, 0.25)', padding: '2px 8px', borderRadius: '4px', background: 'rgba(0, 230, 118, 0.05)' }}>
                            Verified 👑
                          </span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Unverified</span>
                        )}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        <span style={{ 
                          display: 'inline-block',
                          padding: '2px 8px', 
                          borderRadius: '4px', 
                          fontSize: '0.75rem', 
                          fontWeight: 600, 
                          background: su.plan === 'Paid' ? 'rgba(0, 229, 255, 0.15)' : 'rgba(255,255,255,0.05)',
                          color: su.plan === 'Paid' ? 'var(--neon-cyan)' : 'var(--text-secondary)'
                        }}>
                          {su.plan.toUpperCase()} {su.plan === 'Paid' && `(${su.planName})`}
                        </span>
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'right', fontWeight: 600, color: su.talkTimeMins7d > 0 ? 'var(--neon-green)' : 'var(--text-muted)' }}>
                        {su.talkTimeMins7d} mins
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                        <span style={{ 
                          display: 'inline-block',
                          padding: '2px 6px', 
                          borderRadius: '4px', 
                          fontSize: '0.7rem', 
                          fontWeight: 600, 
                          background: 'rgba(0, 230, 118, 0.12)',
                          color: 'var(--neon-green)'
                        }}>
                          ACTIVE
                        </span>
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'right', color: 'var(--text-secondary)' }}>
                        {su.joinedAt}
                      </td>
                      {user?.email === 'mohitjain1619@gmail.com' && (
                        <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                          <button
                            className="btn-glass"
                            style={{ 
                              padding: '6px 12px', 
                              fontSize: '0.72rem', 
                              borderColor: '#ef4444', 
                              color: '#ef4444',
                              background: 'rgba(239, 68, 68, 0.05)'
                            }}
                            onClick={() => handleAdminDeleteUser(su.userId, su.email)}
                            disabled={updatingAdminId === su.userId}
                          >
                            🗑️ Delete User
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ color: 'var(--text-secondary)', padding: '24px 0', textAlign: 'center' }}>
                No matching referral signups found.
              </div>
            )}
          </div>
        </div>

        {/* Admin Panel inside Approved Creator Dashboard */}
        {user?.email === 'mohitjain1619@gmail.com' && renderAdminPanel()}
      </div>
    );
  }

  // ═══════════════════════════════════════════════════════════
  // 2. GUEST, PENDING OR NEW APPLICANT LANDING VIEW (Shared Promo Layout)
  // ═══════════════════════════════════════════════════════════
  return (
    <div className="section" style={{ paddingTop: '100px', paddingBottom: '80px' }}>
      
      {/* Hero Section */}
      <div style={{ textAlign: 'center', maxWidth: '800px', margin: '0 auto', marginBottom: '80px' }}>
        <span className="badge badge-neon" style={{ marginBottom: '24px', padding: '6px 16px', fontSize: '0.85rem' }}>
          💰 Creator Partnership
        </span>
        <h1 style={{ fontSize: 'clamp(2.5rem, 6vw, 4rem)', fontWeight: 900, marginBottom: '24px', lineHeight: 1.1 }}>
          Partner with <span className="neon-text">Camverz</span> <br />
          & Earn <span style={{ color: 'var(--neon-green)' }}>25% Lifetime</span> commission
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.2rem', lineHeight: 1.6, marginBottom: '32px' }}>
          Earn 25% lifetime recurring commissions by recommending the web's premium random video calling platform. Invite users, stream, and get paid directly to your UPI.
        </p>
        <button 
          className="btn-neon" 
          onClick={() => {
            if (isGuest) {
              setShowLogin(true);
            } else {
              document.getElementById('apply-section')?.scrollIntoView({ behavior: 'smooth' });
            }
          }} 
          style={{ padding: '16px 36px', fontSize: '1.1rem' }}
        >
          {user ? "Apply to Join" : "Sign In to Apply"} <ArrowRight size={18} />
        </button>
      </div>

      {/* Benefits Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '24px', marginBottom: '80px' }}>
        <div className="glass-card" style={{ padding: '32px', textAlign: 'center' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '50%', background: 'rgba(0, 230, 118, 0.1)', display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '0 auto 20px' }}>
            <DollarSign size={28} color="var(--neon-green)" />
          </div>
          <h3 style={{ marginBottom: '12px' }}>25% Lifetime Commissions</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>Earn recurring payouts on all coin package refills and premium subscriptions your referrals purchase.</p>
        </div>
        <div className="glass-card" style={{ padding: '32px', textAlign: 'center' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '50%', background: 'rgba(0, 229, 255, 0.1)', display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '0 auto 20px' }}>
            <Clock size={28} color="var(--neon-cyan)" />
          </div>
          <h3 style={{ marginBottom: '12px' }}>30-Day Cookie Window</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>If visitors signup within 30 days of clicking your link, they are credited to your account forever.</p>
        </div>
        <div className="glass-card" style={{ padding: '32px', textAlign: 'center' }}>
          <div style={{ width: '56px', height: '56px', borderRadius: '50%', background: 'rgba(189, 0, 255, 0.1)', display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '0 auto 20px' }}>
            <Zap size={28} color="var(--neon-purple)" />
          </div>
          <h3 style={{ marginBottom: '12px' }}>Instant UPI Payouts</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>No complicated bank forms. Earned money is transferred directly to your preferred UPI address.</p>
        </div>
      </div>

      {/* ═══════════════════════════════════════════════════════════ */}
      {/* INTERACTIVE ACTION BLOCK AT BOTTOM */}
      {/* ═══════════════════════════════════════════════════════════ */}
      <div id="apply-section" style={{ maxWidth: '800px', margin: '0 auto 80px', scrollMarginTop: '100px' }}>
        
        {/* State A: Guest (Not logged in) */}
        {isGuest && (
          <div className="glass-card" style={{ padding: '40px', textAlign: 'center', border: '1px solid rgba(0, 229, 255, 0.15)' }}>
            <Zap size={36} color="var(--neon-cyan)" style={{ marginBottom: '16px', display: 'inline-block' }} />
            <h3 style={{ fontSize: '1.5rem', marginBottom: '10px' }}>Claim Your Creator Link</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '0.95rem' }}>
              Sign in with Google to configure your custom creator referral code and verify your profiles.
            </p>
            <button className="btn-neon" onClick={() => setShowLogin(true)} style={{ padding: '12px 28px' }}>
              Sign In to Become a Creator
            </button>
          </div>
        )}

        {/* State B: Logged in, not applied yet (Beautiful, compact form) */}
        {isNewCreator && (
          <div className="glass-card" style={{ padding: '40px' }}>
            <h3 style={{ fontSize: '1.6rem', marginBottom: '8px' }}>Apply for Creator Program</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '28px', fontSize: '0.95rem' }}>
              Fill in your details below to instantly apply and claim your custom referral code.
            </p>

            {errorMsg && (
              <div style={{ background: 'rgba(255, 0, 110, 0.1)', border: '1px solid rgba(255, 0, 110, 0.3)', padding: '12px 16px', borderRadius: '8px', color: 'var(--neon-pink)', marginBottom: '24px', display: 'flex', gap: '10px', alignItems: 'center', fontSize: '0.9rem' }}>
                <AlertCircle size={18} />
                <span>{errorMsg}</span>
              </div>
            )}

            {successMsg && (
              <div style={{ background: 'rgba(0, 230, 118, 0.1)', border: '1px solid rgba(0, 230, 118, 0.3)', padding: '12px 16px', borderRadius: '8px', color: 'var(--neon-green)', marginBottom: '24px', display: 'flex', gap: '10px', alignItems: 'center', fontSize: '0.9rem' }}>
                <CheckCircle size={18} />
                <span>{successMsg}</span>
              </div>
            )}

            <form onSubmit={handleSubmitApplication} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              
              {/* Form Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>FULL NAME</label>
                  <input 
                    type="text" 
                    className="input-glass" 
                    placeholder="e.g. Rahul Sharma" 
                    value={name} 
                    onChange={(e) => setName(e.target.value)} 
                    required 
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>PREFERRED CODE (3-20 CHARS, ALPHANUMERIC)</label>
                  <input 
                    type="text" 
                    className="input-glass" 
                    placeholder="e.g. RAHUL25" 
                    value={code} 
                    onChange={(e) => setCode(e.target.value.replace(/[^a-zA-Z0-9]/g, ''))} 
                    required 
                  />
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                    Your referrals will use: {siteUrl}?ref={code ? code.toUpperCase() : 'CODE'}
                  </span>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>INSTAGRAM PROFILE URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="e.g. https://instagram.com/username" 
                    value={instagramUrl} 
                    onChange={(e) => setInstagramUrl(e.target.value)} 
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>YOUTUBE CHANNEL URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="e.g. https://youtube.com/@channel" 
                    value={youtubeUrl} 
                    onChange={(e) => setYoutubeUrl(e.target.value)} 
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>OTHER PLATFORM PROFILE URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="e.g. https://example.com/profile/username" 
                    value={otherUrl} 
                    onChange={(e) => setOtherUrl(e.target.value)} 
                  />
                </div>


              </div>

              <label style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', cursor: 'pointer', userSelect: 'none', fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '8px' }}>
                <input 
                  type="checkbox" 
                  checked={confirmOwnership} 
                  onChange={(e) => setConfirmOwnership(e.target.checked)} 
                  style={{ marginTop: '3px' }}
                  required 
                />
                <span>I confirm that the submitted social profiles belong to me and represent my authentic identity.</span>
              </label>

              <button type="submit" className="btn-neon" style={{ width: '100%', marginTop: '10px', padding: '14px' }} disabled={submitting}>
                {submitting ? "Submitting Application..." : "Submit Application"}
              </button>
            </form>
          </div>
        )}

        {/* State C: Logged in & Application pending review */}
        {isPendingCreator && (
          <div className="glass-card" style={{ padding: '40px' }}>
            <div style={{ textAlign: 'center', marginBottom: '32px' }}>
              <div style={{ fontSize: '3.5rem', marginBottom: '20px' }}>📋</div>
              <h3 style={{ fontSize: '1.6rem', marginBottom: '8px' }}>Verify Your Profiles</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem', maxWidth: '600px', margin: '0 auto' }}>
                Your referral code <strong className="neon-text">{affData.affiliate.code}</strong> is reserved!
                Verify ownership of your submitted profiles below. After verification, our team will review and approve.
              </p>
            </div>

            {errorMsg && (
              <div style={{ background: 'rgba(255, 0, 110, 0.1)', border: '1px solid rgba(255, 0, 110, 0.3)', padding: '12px 16px', borderRadius: '8px', color: 'var(--neon-pink)', marginBottom: '28px', display: 'flex', gap: '10px', alignItems: 'center', fontSize: '0.9rem' }}>
                <AlertCircle size={18} />
                <span>{errorMsg}</span>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '30px', marginBottom: '32px' }}>


              {/* Instagram Card */}
              {affData.affiliate.instagram_url && (
                <div className="glass-card" style={{ padding: '28px', background: affData.affiliate.instagram_verified ? 'rgba(0, 230, 118, 0.03)' : 'rgba(255, 255, 255, 0.01)', border: affData.affiliate.instagram_verified ? '1px solid rgba(0, 230, 118, 0.25)' : '1px solid var(--glass-border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <h4 style={{ margin: 0, fontSize: '1.15rem', color: '#fff' }}>📸 Instagram</h4>
                    {affData.affiliate.instagram_verified ? (
                      <span style={{ color: 'var(--neon-green)', fontWeight: 600, fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}><CheckCircle size={16} /> Verified</span>
                    ) : (
                      <span style={{ color: '#f59e0b', fontWeight: 600, fontSize: '0.8rem' }}>⏳ Pending</span>
                    )}
                  </div>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '16px', wordBreak: 'break-all' }}>
                    <a href={affData.affiliate.instagram_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline' }}>{affData.affiliate.instagram_url}</a>
                  </p>
                  {affData.affiliate.instagram_verified ? (
                    <div style={{ background: 'rgba(0,230,118,0.08)', padding: '14px 16px', borderRadius: '10px', color: 'var(--neon-green)', fontSize: '0.85rem', lineHeight: 1.5 }}>
                      ✅ Verified! You can now <strong>remove the code</strong> from your bio.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div style={{ background: 'rgba(255,255,255,0.03)', padding: '16px', borderRadius: '10px', border: '1px solid var(--glass-border)' }}>
                        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '0 0 10px 0', lineHeight: 1.5 }}>
                          <strong style={{ color: '#fff' }}>Step 1:</strong> Copy this code and add it to your <strong>Instagram Bio</strong>:
                        </p>
                        <div style={{ background: 'rgba(255,0,110,0.08)', border: '1px solid rgba(255,0,110,0.2)', padding: '10px 14px', borderRadius: '8px', fontFamily: 'monospace', fontSize: '1rem', color: 'var(--neon-pink)', fontWeight: 700, textAlign: 'center', letterSpacing: '1px', cursor: 'pointer' }}
                          onClick={() => { navigator.clipboard.writeText(affData.affiliate.instagram_bio_code); alert('Code copied!'); }}
                          title="Click to copy"
                        >
                          {affData.affiliate.instagram_bio_code} 📋
                        </div>
                        <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: '10px 0 0 0' }}>
                          <strong style={{ color: '#fff' }}>Step 2:</strong> Click Verify. After success, you can remove the code.
                        </p>
                      </div>
                      <div>
                        <button className="btn-neon" style={{ width: '100%', padding: '14px' }} onClick={() => handleVerifyInstagram()} disabled={verifyingInstagram}>
                          {verifyingInstagram ? "Checking Bio..." : "✓ Verify Instagram"}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* YouTube Card */}
              {affData.affiliate.youtube_url && (
                <div className="glass-card" style={{ padding: '28px', background: affData.affiliate.youtube_verified ? 'rgba(0, 230, 118, 0.03)' : 'rgba(255, 255, 255, 0.01)', border: affData.affiliate.youtube_verified ? '1px solid rgba(0, 230, 118, 0.25)' : '1px solid var(--glass-border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <h4 style={{ margin: 0, fontSize: '1.15rem', color: '#fff' }}>🎬 YouTube</h4>
                    {affData.affiliate.youtube_verified ? (
                      <span style={{ color: 'var(--neon-green)', fontWeight: 600, fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}><CheckCircle size={16} /> Verified</span>
                    ) : (
                      <span style={{ color: '#f59e0b', fontWeight: 600, fontSize: '0.8rem' }}>⏳ Pending</span>
                    )}
                  </div>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '16px', wordBreak: 'break-all' }}>
                    <a href={affData.affiliate.youtube_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline' }}>{affData.affiliate.youtube_url}</a>
                  </p>
                  {affData.affiliate.youtube_verified ? (
                    <div style={{ background: 'rgba(0,230,118,0.08)', padding: '14px 16px', borderRadius: '10px', color: 'var(--neon-green)', fontSize: '0.85rem', lineHeight: 1.5 }}>
                      ✅ Verified! You can now <strong>remove the code</strong> from your channel description.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div style={{ background: 'rgba(255,255,255,0.03)', padding: '16px', borderRadius: '10px', border: '1px solid var(--glass-border)' }}>
                        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '0 0 10px 0', lineHeight: 1.5 }}>
                          <strong style={{ color: '#fff' }}>Step 1:</strong> Copy this code and add it to your <strong>YouTube Channel Description</strong>:
                        </p>
                        <div style={{ background: 'rgba(255,0,110,0.08)', border: '1px solid rgba(255,0,110,0.2)', padding: '10px 14px', borderRadius: '8px', fontFamily: 'monospace', fontSize: '1rem', color: 'var(--neon-pink)', fontWeight: 700, textAlign: 'center', letterSpacing: '1px', cursor: 'pointer' }}
                          onClick={() => { navigator.clipboard.writeText(affData.affiliate.youtube_bio_code); alert('Code copied!'); }}
                          title="Click to copy"
                        >
                          {affData.affiliate.youtube_bio_code} 📋
                        </div>
                        <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: '10px 0 0 0' }}>
                          <strong style={{ color: '#fff' }}>Step 2:</strong> Click Verify. After success, you can remove the code.
                        </p>
                      </div>
                      <div>
                        <button className="btn-neon" style={{ width: '100%', padding: '14px' }} onClick={() => handleVerifyYoutube()} disabled={verifyingYoutube}>
                          {verifyingYoutube ? "Checking Description..." : "✓ Verify YouTube"}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Other Platform Card */}
              {affData.affiliate.other_url && (
                <div className="glass-card" style={{ padding: '28px', background: affData.affiliate.other_verified ? 'rgba(0, 230, 118, 0.03)' : 'rgba(255, 255, 255, 0.01)', border: affData.affiliate.other_verified ? '1px solid rgba(0, 230, 118, 0.25)' : '1px solid var(--glass-border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <h4 style={{ margin: 0, fontSize: '1.15rem', color: '#fff' }}>🌐 Other Platform</h4>
                    {affData.affiliate.other_verified ? (
                      <span style={{ color: 'var(--neon-green)', fontWeight: 600, fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}><CheckCircle size={16} /> Verified</span>
                    ) : (
                      <span style={{ color: '#f59e0b', fontWeight: 600, fontSize: '0.8rem' }}>⏳ Pending</span>
                    )}
                  </div>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '16px', wordBreak: 'break-all' }}>
                    <a href={affData.affiliate.other_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline' }}>{affData.affiliate.other_url}</a>
                  </p>
                  {affData.affiliate.other_verified ? (
                    <div style={{ background: 'rgba(0,230,118,0.08)', padding: '14px 16px', borderRadius: '10px', color: 'var(--neon-green)', fontSize: '0.85rem', lineHeight: 1.5 }}>
                      ✅ Verified! You can now <strong>remove the code</strong> from your profile/bio.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div style={{ background: 'rgba(255,255,255,0.03)', padding: '16px', borderRadius: '10px', border: '1px solid var(--glass-border)' }}>
                        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '0 0 10px 0', lineHeight: 1.5 }}>
                          {(() => {
                            const url = (affData.affiliate.other_url || "").toLowerCase();
                            if (url.includes("xhamster")) {
                              return (
                                <>
                                  <strong style={{ color: '#fff' }}>Step 1:</strong> Switch your browser to <strong>Desktop Mode</strong>, go to your <strong>xHamster Creator Profile's About/Description section</strong>, and temporarily paste this code there:
                                </>
                              );
                            }
                            if (url.includes("faphouse")) {
                              return (
                                <>
                                  <strong style={{ color: '#fff' }}>Step 1:</strong> Switch your browser to <strong>Desktop Mode</strong>, go to your <strong>Faphouse Creator Profile's Bio/About section</strong>, and temporarily paste this code there:
                                </>
                              );
                            }
                            return (
                              <>
                                <strong style={{ color: '#fff' }}>Step 1:</strong> Copy this code and add it to your <strong>Profile Bio/Description</strong>:
                              </>
                            );
                          })()}
                        </p>
                        <div style={{ background: 'rgba(255,0,110,0.08)', border: '1px solid rgba(255,0,110,0.2)', padding: '10px 14px', borderRadius: '8px', fontFamily: 'monospace', fontSize: '1rem', color: 'var(--neon-pink)', fontWeight: 700, textAlign: 'center', letterSpacing: '1px', cursor: 'pointer' }}
                          onClick={() => { navigator.clipboard.writeText(affData.affiliate.other_bio_code); alert('Code copied!'); }}
                          title="Click to copy"
                        >
                          {affData.affiliate.other_bio_code} 📋
                        </div>
                        <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', margin: '10px 0 0 0' }}>
                          <strong style={{ color: '#fff' }}>Step 2:</strong> Click Verify. After success, you can remove the code.
                        </p>
                      </div>
                      <div>
                        <button className="btn-neon" style={{ width: '100%', padding: '14px' }} onClick={() => handleVerifyOther()} disabled={verifyingOther}>
                          {verifyingOther ? "Checking Profile..." : "✓ Verify Profile"}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}

            </div>

            {((!affData.affiliate.instagram_url || affData.affiliate.instagram_verified) &&
              (!affData.affiliate.youtube_url || affData.affiliate.youtube_verified) &&
              (!affData.affiliate.other_url || affData.affiliate.other_verified)) && (
              <div style={{ background: 'rgba(0, 230, 118, 0.06)', border: '1px solid rgba(0, 230, 118, 0.2)', padding: '16px 20px', borderRadius: '12px', marginBottom: '24px', textAlign: 'center' }}>
                <p style={{ color: 'var(--neon-green)', fontWeight: 600, fontSize: '0.95rem', margin: 0 }}>
                  🎉 All submitted profiles verified! Your application is now under admin review.
                </p>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <button className="btn-glass" onClick={handleResetVerification} disabled={resetting} style={{ fontSize: '0.85rem' }}>
                {resetting ? "Resetting..." : "Reset Application & Edit URLs"}
              </button>
            </div>
          </div>
        )}



      </div>

      {/* FAQs */}
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        <h2 className="text-center" style={{ marginBottom: '40px' }}>Frequently Asked Questions</h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {faqs.map((faq, idx) => (
            <div 
              key={idx} 
              className="glass-card" 
              style={{ padding: '20px', cursor: 'pointer' }}
              onClick={() => setOpenFaq(openFaq === idx ? null : idx)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h4 style={{ margin: 0, fontSize: '1.1rem' }}>{faq.q}</h4>
                <ChevronDown 
                  size={18} 
                  style={{ 
                    transform: openFaq === idx ? 'rotate(180deg)' : 'rotate(0deg)',
                    transition: 'transform var(--transition-normal)'
                  }} 
                />
              </div>
              {openFaq === idx && (
                <p style={{ marginTop: '12px', color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: 1.5 }}>
                  {faq.a}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Admin Panel inside Guest / Pending Creator view */}
      {user?.email === 'mohitjain1619@gmail.com' && (
        <div style={{ maxWidth: '800px', margin: '60px auto 0' }}>
          {renderAdminPanel()}
        </div>
      )}

    </div>
  );
}
