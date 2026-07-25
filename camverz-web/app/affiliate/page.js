'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '../../lib/auth-context';
import { 
  getAffiliateMe, 
  applyAffiliate, 
  verifyAffiliateBio, 
  resetAffiliateVerification,
  verifyInstagramBio,
  verifyYoutubeBio
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
  const [copied, setCopied] = useState(false);
  
  // Application Form States
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [upiId, setUpiId] = useState('');
  const [instagramUrl, setInstagramUrl] = useState('');
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [confirmOwnership, setConfirmOwnership] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Verification States
  const [verifying, setVerifying] = useState(false);
  const [verifyingInstagram, setVerifyingInstagram] = useState(false);
  const [verifyingYoutube, setVerifyingYoutube] = useState(false);
  const [resetting, setResetting] = useState(false);

  // FAQ Accordion State
  const [openFaq, setOpenFaq] = useState(null);

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

  useEffect(() => {
    loadAffiliateData();
  }, [user]);

  // Copy to clipboard handler
  const handleCopyLink = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Submit Creator Application
  const handleSubmitApplication = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!name.trim() || !code.trim() || !instagramUrl.trim() || !youtubeUrl.trim()) {
      setErrorMsg("All fields are required.");
      return;
    }
    if (!confirmOwnership) {
      setErrorMsg("You must confirm ownership of the social profiles.");
      return;
    }

    const upiRegex = /^[\w.\-]{2,}@[a-zA-Z]{2,}$/;
    if (upiId.trim() && !upiRegex.test(upiId.trim())) {
      setErrorMsg("Invalid UPI ID format (e.g. name@bank).");
      return;
    }

    const combinedSocialUrl = `Instagram: ${instagramUrl.trim()} | YouTube: ${youtubeUrl.trim()}`;

    try {
      setSubmitting(true);
      const response = await applyAffiliate({
        name: name.trim(),
        preferredCode: code.trim().toUpperCase(),
        upiId: upiId.trim() || null,
        socialUrl: combinedSocialUrl,
        confirmOwnership
      });

      if (response.status === 'success') {
        setSuccessMsg(response.message);
        setName('');
        setCode('');
        setUpiId('');
        setInstagramUrl('');
        setYoutubeUrl('');
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

  const handleVerifyInstagram = async (simulate = false) => {
    setErrorMsg('');
    try {
      setVerifyingInstagram(true);
      const response = await verifyInstagramBio(simulate);
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

  const handleVerifyYoutube = async (simulate = false) => {
    setErrorMsg('');
    try {
      setVerifyingYoutube(true);
      const response = await verifyYoutubeBio(simulate);
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

  const siteUrl = typeof window !== 'undefined' ? window.location.origin : 'https://camverz-nine.vercel.app';
  const referralLink = affData?.affiliate ? `${siteUrl}?ref=${affData.affiliate.code}` : '';

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

    return (
      <div className="section" style={{ paddingTop: '90px', paddingBottom: '60px' }}>
        {/* Welcome Banner */}
        <div className="glass-card" style={{ padding: '30px', marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '20px' }}>
          <div>
            <h2 style={{ marginBottom: '6px' }}>Welcome, {affiliate.name}!</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
              Code: <strong style={{ color: 'var(--neon-green)' }}>{affiliate.code}</strong> | Status: <span style={{ color: 'var(--neon-green)', fontWeight: 600 }}>Active</span>
            </p>
          </div>
          
          {/* Copy Referral Link */}
          <div style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid var(--glass-border)', padding: '12px 20px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 600 }}>YOUR REFERRAL LINK</span>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-primary)', fontFamily: 'monospace' }}>{referralLink}</span>
            </div>
            <button 
              onClick={() => handleCopyLink(referralLink)} 
              style={{ 
                width: '40px', 
                height: '40px', 
                borderRadius: '8px', 
                background: copied ? 'rgba(0, 230, 118, 0.15)' : 'var(--glass-bg)', 
                border: `1px solid ${copied ? 'var(--neon-green)' : 'var(--glass-border)'}`,
                display: 'flex', 
                justifyContent: 'center', 
                alignItems: 'center',
                color: copied ? 'var(--neon-green)' : 'var(--neon-cyan)',
                cursor: 'pointer',
                transition: 'all 200ms ease'
              }}
            >
              {copied ? <Check size={18} /> : <Copy size={18} />}
            </button>
          </div>
        </div>

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
                    required 
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
                    required 
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>UPI ID FOR PAYOUTS (OPTIONAL)</label>
                  <input 
                    type="text" 
                    className="input-glass" 
                    placeholder="e.g. username@paytm" 
                    value={upiId} 
                    onChange={(e) => setUpiId(e.target.value)} 
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
                <span>I confirm that the submitted Instagram & YouTube profiles belong to me and represent my authentic public identity.</span>
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
                Verify ownership of your Instagram &amp; YouTube accounts below. After verification, our team will review and approve.
              </p>
            </div>

            {errorMsg && (
              <div style={{ background: 'rgba(255, 0, 110, 0.1)', border: '1px solid rgba(255, 0, 110, 0.3)', padding: '12px 16px', borderRadius: '8px', color: 'var(--neon-pink)', marginBottom: '28px', display: 'flex', gap: '10px', alignItems: 'center', fontSize: '0.9rem' }}>
                <AlertCircle size={18} />
                <span>{errorMsg}</span>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '30px', marginBottom: '32px' }}>
              
              {/* Instagram Verification */}
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
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button className="btn-neon" style={{ flex: 1, padding: '12px' }} onClick={() => handleVerifyInstagram(false)} disabled={verifyingInstagram}>
                        {verifyingInstagram ? "Checking Bio..." : "✓ Verify Instagram"}
                      </button>
                      <button className="btn-glass" style={{ borderColor: 'var(--neon-purple)', fontSize: '0.75rem', padding: '10px 14px' }} onClick={() => handleVerifyInstagram(true)} disabled={verifyingInstagram}>
                        Test
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* YouTube Verification */}
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
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button className="btn-neon" style={{ flex: 1, padding: '12px' }} onClick={() => handleVerifyYoutube(false)} disabled={verifyingYoutube}>
                        {verifyingYoutube ? "Checking Description..." : "✓ Verify YouTube"}
                      </button>
                      <button className="btn-glass" style={{ borderColor: 'var(--neon-purple)', fontSize: '0.75rem', padding: '10px 14px' }} onClick={() => handleVerifyYoutube(true)} disabled={verifyingYoutube}>
                        Test
                      </button>
                    </div>
                  </div>
                )}
              </div>

            </div>

            {affData.affiliate.instagram_verified && affData.affiliate.youtube_verified && (
              <div style={{ background: 'rgba(0, 230, 118, 0.06)', border: '1px solid rgba(0, 230, 118, 0.2)', padding: '16px 20px', borderRadius: '12px', marginBottom: '24px', textAlign: 'center' }}>
                <p style={{ color: 'var(--neon-green)', fontWeight: 600, fontSize: '0.95rem', margin: 0 }}>
                  🎉 Both profiles verified! Your application is now under admin review.
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

    </div>
  );
}
