'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '../../lib/auth-context';
import { 
  getAffiliateMe, 
  applyAffiliate, 
  verifyAffiliateBio, 
  resetAffiliateVerification,
  getAdminAffiliates,
  approveAffiliate,
  updateAffiliateAdmin,
  getAdminSales,
  recordAdminPayout
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
  ChevronDown,
  Calendar,
  Crown,
  Settings,
  Activity,
  Edit2
} from 'lucide-react';

export default function AffiliatePage() {
  const { user, loading: authLoading, setShowLogin } = useAuth();
  
  // App States
  const [affData, setAffData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const [viewMode, setViewMode] = useState('creator'); // 'creator' or 'admin'
  
  // Application Form States
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [upiId, setUpiId] = useState('');
  const [socialUrl, setSocialUrl] = useState('');
  const [confirmOwnership, setConfirmOwnership] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Verification States
  const [verifying, setVerifying] = useState(false);
  const [resetting, setResetting] = useState(false);

  // FAQ Accordion State
  const [openFaq, setOpenFaq] = useState(null);

  // Admin Dashboard States
  const [adminCreators, setAdminCreators] = useState([]);
  const [adminSales, setAdminSales] = useState([]);
  const [adminTab, setAdminTab] = useState('creators'); // 'creators' or 'sales'
  const [adminLoading, setAdminLoading] = useState(false);

  // Admin Action States (Modals/Forms)
  const [activePayoutCreator, setActivePayoutCreator] = useState(null);
  const [payoutAmount, setPayoutAmount] = useState('');
  const [payoutMethod, setPayoutMethod] = useState('upi');
  const [payoutTxRef, setPayoutTxRef] = useState('');
  const [payoutNotes, setPayoutNotes] = useState('');
  const [payoutSubmitting, setPayoutSubmitting] = useState(false);

  const [activeEditCreator, setActiveEditCreator] = useState(null);
  const [editStatus, setEditStatus] = useState('approved');
  const [editRate, setEditRate] = useState('0.25');
  const [editMinPayout, setEditMinPayout] = useState('8000');
  const [editNotes, setEditNotes] = useState('');
  const [editSubmitting, setEditSubmitting] = useState(false);

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

  // Fetch Admin Data
  const loadAdminData = async () => {
    if (user?.email !== 'mohitjain1619@gmail.com') return;
    try {
      setAdminLoading(true);
      const [creators, sales] = await Promise.all([
        getAdminAffiliates(),
        getAdminSales()
      ]);
      setAdminCreators(creators);
      setAdminSales(sales);
    } catch (err) {
      console.error("Failed to load admin data:", err);
    } finally {
      setAdminLoading(false);
    }
  };

  useEffect(() => {
    loadAffiliateData();
  }, [user]);

  useEffect(() => {
    if (viewMode === 'admin') {
      loadAdminData();
    }
  }, [viewMode]);

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

    if (!name.trim() || !code.trim() || !socialUrl.trim()) {
      setErrorMsg("All fields are required.");
      return;
    }
    if (!confirmOwnership) {
      setErrorMsg("You must confirm ownership of the social profile.");
      return;
    }

    const upiRegex = /^[\w.\-]{2,}@[a-zA-Z]{2,}$/;
    if (upiId.trim() && !upiRegex.test(upiId.trim())) {
      setErrorMsg("Invalid UPI ID format (e.g. name@bank).");
      return;
    }

    try {
      setSubmitting(true);
      const response = await applyAffiliate({
        name: name.trim(),
        preferredCode: code.trim().toUpperCase(),
        upiId: upiId.trim() || null,
        socialUrl: socialUrl.trim(),
        confirmOwnership
      });

      if (response.status === 'success') {
        setSuccessMsg(response.message);
        setName('');
        setCode('');
        setUpiId('');
        setSocialUrl('');
        setConfirmOwnership(false);
        await loadAffiliateData();
        document.getElementById('action-card')?.scrollIntoView({ behavior: 'smooth' });
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.error || "Application submission failed. Please try another code.");
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

  // Admin: Approve Creator Application
  const handleApproveCreator = async (creatorId) => {
    if (!window.confirm("Are you sure you want to approve this creator?")) return;
    try {
      await approveAffiliate(creatorId);
      alert("Creator approved successfully!");
      await loadAdminData();
    } catch (err) {
      alert("Failed to approve creator.");
    }
  };

  // Admin: Submit Creator Updates
  const handleSaveCreatorEdit = async (e) => {
    e.preventDefault();
    if (!activeEditCreator) return;
    try {
      setEditSubmitting(true);
      await updateAffiliateAdmin(activeEditCreator.id, {
        status: editStatus,
        commissionRate: parseFloat(editRate),
        minPayout: parseInt(editMinPayout, 10),
        adminNotes: editNotes
      });
      alert("Creator settings updated!");
      setActiveEditCreator(null);
      await loadAdminData();
    } catch (err) {
      alert("Failed to update creator details.");
    } finally {
      setEditSubmitting(false);
    }
  };

  // Admin: Submit Payout Entry
  const handleSavePayout = async (e) => {
    e.preventDefault();
    if (!activePayoutCreator) return;
    if (!payoutAmount || isNaN(payoutAmount)) {
      alert("Please enter a valid amount.");
      return;
    }
    try {
      setPayoutSubmitting(true);
      await recordAdminPayout({
        affiliateId: activePayoutCreator.id,
        amount: parseFloat(payoutAmount),
        method: payoutMethod,
        transactionRef: payoutTxRef,
        adminNotes: payoutNotes
      });
      alert("Payout recorded successfully!");
      setActivePayoutCreator(null);
      setPayoutAmount('');
      setPayoutTxRef('');
      setPayoutNotes('');
      await loadAdminData();
    } catch (err) {
      alert("Failed to record payout.");
    } finally {
      setPayoutSubmitting(false);
    }
  };

  const siteUrl = typeof window !== 'undefined' ? window.location.origin : 'https://camverz-nine.vercel.app';
  const referralLink = affData?.affiliate ? `${siteUrl}?ref=${affData.affiliate.code}` : '';

  const isAdmin = user && user.email === 'mohitjain1619@gmail.com';
  const isApprovedCreator = user && affData?.has_affiliate && affData.affiliate.status === 'approved';

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

  // ═══════════════════════════════════════════════════════════
  // 1. ADMIN VIEW (Control Center Panel)
  // ═══════════════════════════════════════════════════════════
  if (isAdmin && viewMode === 'admin') {
    return (
      <div className="section" style={{ paddingTop: '90px', paddingBottom: '60px' }}>
        
        {/* Toggle View Header */}
        <div className="glass-card" style={{ padding: '24px', marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '20px' }}>
          <div>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Crown size={28} color="var(--neon-purple)" />
              <span>Affiliate Admin Center</span>
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Control rates, approvals, audits, and payouts.</p>
          </div>
          <button className="btn-neon" onClick={() => setViewMode('creator')} style={{ background: 'var(--gradient-neon)' }}>
            Switch to Creator View
          </button>
        </div>

        {/* Tab Selectors */}
        <div style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
          <button 
            className={adminTab === 'creators' ? 'btn-neon' : 'btn-glass'} 
            onClick={() => setAdminTab('creators')}
            style={{ padding: '8px 20px', fontSize: '0.85rem' }}
          >
            Creators List
          </button>
          <button 
            className={adminTab === 'sales' ? 'btn-neon' : 'btn-glass'} 
            onClick={() => setAdminTab('sales')}
            style={{ padding: '8px 20px', fontSize: '0.85rem' }}
          >
            Sales Logs
          </button>
        </div>

        {adminLoading ? (
          <div style={{ padding: '50px 0', textAlign: 'center', color: 'var(--neon-cyan)' }}>Loading admin statistics...</div>
        ) : adminTab === 'creators' ? (
          // Tab 1: Creators List
          <div className="glass-card" style={{ padding: '24px', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--glass-border)', color: 'var(--text-muted)' }}>
                  <th style={{ textAlign: 'left', padding: '12px 8px' }}>CREATOR / CODE</th>
                  <th style={{ textAlign: 'left', padding: '12px 8px' }}>EMAIL / SOCIAL</th>
                  <th style={{ textAlign: 'center', padding: '12px 8px' }}>CLICKS/SIGNS</th>
                  <th style={{ textAlign: 'right', padding: '12px 8px' }}>EARNINGS</th>
                  <th style={{ textAlign: 'right', padding: '12px 8px' }}>PAID / PENDING</th>
                  <th style={{ textAlign: 'center', padding: '12px 8px' }}>STATUS</th>
                  <th style={{ textAlign: 'center', padding: '12px 8px' }}>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {adminCreators.map(a => (
                  <tr key={a.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <td style={{ padding: '12px 8px' }}>
                      <div style={{ fontWeight: 600 }}>{a.name}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--neon-cyan)' }}>{a.code}</div>
                    </td>
                    <td style={{ padding: '12px 8px' }}>
                      <div style={{ color: 'var(--text-secondary)' }}>{a.email}</div>
                      <a href={a.social_url} target="_blank" rel="noopener noreferrer" style={{ fontSize: '0.75rem', textDecoration: 'underline' }}>LinkedIn Profile</a>
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                      <div>Clicks: {a.clicks}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Signups: {a.signups}</div>
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'right' }}>
                      <div style={{ fontWeight: 600 }}>₹{a.total_earnings}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Rate: {(a.commission_rate * 100)}%</div>
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'right' }}>
                      <div style={{ color: 'var(--text-secondary)' }}>Paid: ₹{a.total_paid}</div>
                      <div style={{ fontWeight: 600, color: a.pending >= a.min_payout ? 'var(--neon-green)' : 'var(--text-muted)' }}>Pend: ₹{a.pending}</div>
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                      <span style={{ 
                        padding: '2px 8px', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 600,
                        background: a.status === 'approved' ? 'rgba(0,230,118,0.15)' : a.status === 'suspended' ? 'rgba(255,0,110,0.15)' : 'rgba(255,234,0,0.15)',
                        color: a.status === 'approved' ? 'var(--neon-green)' : a.status === 'suspended' ? 'var(--neon-pink)' : '#ffea00'
                      }}>
                        {a.status.toUpperCase()}
                      </span>
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                      <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                        {a.status === 'pending' && (
                          <button className="btn-glass" onClick={() => handleApproveCreator(a.id)} style={{ padding: '4px 10px', fontSize: '0.75rem', borderColor: 'var(--neon-green)' }}>
                            Approve
                          </button>
                        )}
                        <button className="btn-glass" onClick={() => {
                          setActiveEditCreator(a);
                          setEditStatus(a.status);
                          setEditRate(String(a.commission_rate));
                          setEditMinPayout(String(a.min_payout));
                          setEditNotes(a.admin_notes || '');
                        }} style={{ padding: '4px 8px' }}>
                          <Edit2 size={12} />
                        </button>
                        {a.status === 'approved' && (
                          <button className="btn-glass" onClick={() => {
                            setActivePayoutCreator(a);
                            setPayoutAmount(String(a.pending));
                          }} style={{ padding: '4px 8px', borderColor: 'var(--neon-cyan)', color: 'var(--neon-cyan)', fontSize: '0.75rem' }}>
                            Payout
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          // Tab 2: Sales Logs
          <div className="glass-card" style={{ padding: '24px', overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--glass-border)', color: 'var(--text-muted)' }}>
                  <th style={{ textAlign: 'left', padding: '12px 8px' }}>TX DATE / REF</th>
                  <th style={{ textAlign: 'left', padding: '12px 8px' }}>CREATOR CODE</th>
                  <th style={{ textAlign: 'left', padding: '12px 8px' }}>CUSTOMER EMAIL</th>
                  <th style={{ textAlign: 'left', padding: '12px 8px' }}>PLAN</th>
                  <th style={{ textAlign: 'right', padding: '12px 8px' }}>AMOUNT</th>
                  <th style={{ textAlign: 'right', padding: '12px 8px' }}>COMMISSION</th>
                  <th style={{ textAlign: 'center', padding: '12px 8px' }}>STATUS</th>
                </tr>
              </thead>
              <tbody>
                {adminSales.map(s => (
                  <tr key={s.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <td style={{ padding: '12px 8px' }}>
                      <div>{s.date}</div>
                      <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: 'var(--text-muted)' }}>{s.razorpay_id}</div>
                    </td>
                    <td style={{ padding: '12px 8px', fontWeight: 600, color: 'var(--neon-cyan)' }}>{s.affiliate_code}</td>
                    <td style={{ padding: '12px 8px', color: 'var(--text-secondary)' }}>{s.customer_email}</td>
                    <td style={{ padding: '12px 8px' }}>{s.plan}</td>
                    <td style={{ padding: '12px 8px', textAlign: 'right', color: 'var(--text-secondary)' }}>₹{s.amount}</td>
                    <td style={{ padding: '12px 8px', textAlign: 'right', fontWeight: 600, color: s.status === 'refunded' ? 'var(--neon-pink)' : 'var(--neon-green)' }}>
                      {s.status === 'refunded' ? `-₹${s.commission_clawback}` : `₹${s.commission}`}
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                      <span style={{ 
                        padding: '2px 8px', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 600,
                        background: s.status === 'refunded' ? 'rgba(255,0,110,0.15)' : 'rgba(0,230,118,0.15)',
                        color: s.status === 'refunded' ? 'var(--neon-pink)' : 'var(--neon-green)'
                      }}>
                        {s.status.toUpperCase()}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* MODAL 1: Edit Creator Details */}
        {activeEditCreator && (
          <div className="modal-overlay">
            <div className="modal-content" style={{ maxWidth: '500px' }}>
              <h3>Edit Creator Settings</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '20px' }}>Adjust configurations for {activeEditCreator.name} ({activeEditCreator.code})</p>
              
              <form onSubmit={handleSaveCreatorEdit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>CREATOR STATUS</label>
                  <select className="input-glass" value={editStatus} onChange={(e) => setEditStatus(e.target.value)} style={{ background: 'var(--bg-surface)' }}>
                    <option value="pending">Pending Verification</option>
                    <option value="approved">Approved / Active</option>
                    <option value="suspended">Suspended</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>COMMISSION RATE (e.g. 0.25 = 25%)</label>
                  <input type="number" step="0.01" className="input-glass" value={editRate} onChange={(e) => setEditRate(e.target.value)} required />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>MINIMUM PAYOUT LIMIT (INR)</label>
                  <input type="number" className="input-glass" value={editMinPayout} onChange={(e) => setEditMinPayout(e.target.value)} required />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>ADMIN PRIVATE NOTES</label>
                  <textarea className="input-glass" value={editNotes} onChange={(e) => setEditNotes(e.target.value)} rows="3" placeholder="Verify bio credentials..." />
                </div>

                <div style={{ display: 'flex', gap: '12px', marginTop: '10px' }}>
                  <button type="submit" className="btn-neon" style={{ flex: 1 }} disabled={editSubmitting}>
                    {editSubmitting ? "Saving..." : "Save Settings"}
                  </button>
                  <button type="button" className="btn-glass" style={{ flex: 1 }} onClick={() => setActiveEditCreator(null)}>
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* MODAL 2: Record Manual Payout */}
        {activePayoutCreator && (
          <div className="modal-overlay">
            <div className="modal-content" style={{ maxWidth: '500px' }}>
              <h3>Record Creator Payout</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '20px' }}>Record a cash payout for {activePayoutCreator.name} ({activePayoutCreator.code})</p>
              
              <form onSubmit={handleSavePayout} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>UPI ID (FROM CREATOR APPLICATION)</label>
                  <input type="text" className="input-glass" value={activePayoutCreator.upi_id || 'Not Provided'} disabled />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>PAYOUT AMOUNT (INR)</label>
                  <input type="number" className="input-glass" value={payoutAmount} onChange={(e) => setPayoutAmount(e.target.value)} required />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>PAYOUT METHOD</label>
                  <select className="input-glass" value={payoutMethod} onChange={(e) => setPayoutMethod(e.target.value)} style={{ background: 'var(--bg-surface)' }}>
                    <option value="upi">UPI Transfer</option>
                    <option value="bank_transfer">Bank Transfer</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>TRANSACTION REFERENCE ID</label>
                  <input type="text" className="input-glass" placeholder="e.g. UTR / UPI reference ID" value={payoutTxRef} onChange={(e) => setPayoutTxRef(e.target.value)} />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '6px' }}>PAYOUT NOTES / DESCRIPTION</label>
                  <textarea className="input-glass" value={payoutNotes} onChange={(e) => setPayoutNotes(e.target.value)} rows="2" placeholder="Reconciled monthly payout..." />
                </div>

                <div style={{ display: 'flex', gap: '12px', marginTop: '10px' }}>
                  <button type="submit" className="btn-neon" style={{ flex: 1 }} disabled={payoutSubmitting}>
                    {payoutSubmitting ? "Recording..." : "Record Payment"}
                  </button>
                  <button type="button" className="btn-glass" style={{ flex: 1 }} onClick={() => setActivePayoutCreator(null)}>
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

      </div>
    );
  }

  // ═══════════════════════════════════════════════════════════
  // 2. CREATOR DASHBOARD VIEW (Approved Creator Dashboard)
  // ═══════════════════════════════════════════════════════════
  if (isApprovedCreator) {
    const { affiliate, stats, recent_sales, payouts, click_chart } = affData;

    return (
      <div className="section" style={{ paddingTop: '90px', paddingBottom: '60px' }}>
        
        {/* Switch to Admin Toggle */}
        {isAdmin && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
            <button className="btn-glass" onClick={() => setViewMode('admin')} style={{ borderColor: 'var(--neon-purple)', color: 'var(--neon-purple)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Crown size={16} /> Switch to Admin Dashboard
            </button>
          </div>
        )}

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
            <div style={{ padding: '40px 0', color: 'var(--text-secondary)', fontSize: '0.95rem' }} className="text-center">
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
  // 3. GUEST, APPLICANT, OR PENDING STATE VIEW (Landing + Action at bottom)
  // ═══════════════════════════════════════════════════════════
  return (
    <div className="section" style={{ paddingTop: '100px', paddingBottom: '80px' }}>
      
      {/* Switch to Admin Toggle (visible on landing if admin) */}
      {isAdmin && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
          <button className="btn-glass" onClick={() => setViewMode('admin')} style={{ borderColor: 'var(--neon-purple)', color: 'var(--neon-purple)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Crown size={16} /> Switch to Admin Dashboard
          </button>
        </div>
      )}

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
            const section = document.getElementById('action-card');
            if (section) {
              section.scrollIntoView({ behavior: 'smooth' });
            } else if (!user) {
              setShowLogin(true);
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
      {/* INTERACTIVE ACTION SECTION */}
      {/* ═══════════════════════════════════════════════════════════ */}
      <div id="action-card" style={{ maxWidth: '800px', margin: '0 auto 80px', scrollMarginTop: '100px' }}>
        
        {/* Case A: Not logged in */}
        {!user && (
          <div className="glass-card" style={{ padding: '40px', textAlign: 'center', border: '1px solid rgba(0, 229, 255, 0.2)' }}>
            <Crown size={40} color="var(--neon-cyan)" style={{ marginBottom: '16px' }} />
            <h3 style={{ fontSize: '1.6rem', marginBottom: '8px' }}>Start Earning Today</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '0.95rem' }}>
              Create an account or sign in with Google to apply for the Creator Program and get your referral link.
            </p>
            <button className="btn-neon" onClick={() => setShowLogin(true)} style={{ padding: '12px 28px' }}>
              Sign In to Become a Creator
            </button>
          </div>
        )}

        {/* Case B: Logged in, not applied yet (Compact Form Grid) */}
        {user && (!affData || !affData.has_affiliate) && (
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
              
              {/* Form Input Grid */}
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
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>PREFERRED CODE</label>
                  <input 
                    type="text" 
                    className="input-glass" 
                    placeholder="e.g. RAHUL25" 
                    value={code} 
                    onChange={(e) => setCode(e.target.value.replace(/[^a-zA-Z0-9]/g, ''))} 
                    required 
                  />
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block', marginTop: '4px' }}>
                    Your link: {siteUrl}?ref={code ? code.toUpperCase() : 'CODE'}
                  </span>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px', letterSpacing: '0.05em' }}>LINKEDIN PROFILE URL</label>
                  <input 
                    type="url" 
                    className="input-glass" 
                    placeholder="https://linkedin.com/in/username" 
                    value={socialUrl} 
                    onChange={(e) => setSocialUrl(e.target.value)} 
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
                <span>I confirm that this LinkedIn profile belongs to me and represents my authentic identity.</span>
              </label>

              <button type="submit" className="btn-neon" style={{ width: '100%', marginTop: '10px', padding: '14px' }} disabled={submitting}>
                {submitting ? "Submitting Application..." : "Submit Application"}
              </button>
            </form>
          </div>
        )}

        {/* Case C: Applied but pending review */}
        {user && affData?.has_affiliate && affData.affiliate.status === 'pending' && (
          <div className="glass-card" style={{ padding: '40px', textAlign: 'center' }}>
            <div style={{ fontSize: '3.5rem', marginBottom: '20px' }}>⏳</div>
            <h3 style={{ fontSize: '1.6rem', marginBottom: '8px' }}>Application Under Review</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '24px', fontSize: '0.95rem' }}>
              Your application with code <strong className="neon-text">{affData.affiliate.code}</strong> is submitted.
              To confirm ownership of the profile (<a href={affData.affiliate.social_url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--neon-cyan)', textDecoration: 'underline' }}>{affData.affiliate.social_url}</a>), complete the bio verification details below.
            </p>

            <div style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid var(--glass-border)', borderRadius: '12px', padding: '24px', marginBottom: '28px', textAlign: 'left' }}>
              <h4 style={{ marginBottom: '12px', color: '#white', display: 'flex', gap: '8px', alignItems: 'center' }}>
                <Shield size={18} color="var(--neon-cyan)" />
                <span>Verification Instructions</span>
              </h4>
              <ol style={{ paddingLeft: '20px', color: 'var(--text-secondary)', fontSize: '0.85rem', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <li>
                  Copy this code: <strong style={{ color: 'var(--neon-pink)', background: 'rgba(255,0,110,0.1)', padding: '2px 8px', borderRadius: '4px', fontFamily: 'monospace' }}>{affData.affiliate.linkedin_bio_code}</strong>
                </li>
                <li>Add it to your LinkedIn profile About/Bio section.</li>
                <li>Click <strong>Verify Profile Bio</strong> to automate validation.</li>
              </ol>
            </div>

            {errorMsg && (
              <div style={{ background: 'rgba(255, 0, 110, 0.1)', border: '1px solid rgba(255, 0, 110, 0.3)', padding: '12px 16px', borderRadius: '8px', color: 'var(--neon-pink)', marginBottom: '24px', display: 'flex', gap: '10px', alignItems: 'center', fontSize: '0.9rem', textAlign: 'left' }}>
                <AlertCircle size={18} />
                <span>{errorMsg}</span>
              </div>
            )}

            <div style={{ display: 'flex', gap: '16px', justifyContent: 'center', flexWrap: 'wrap' }}>
              <button 
                className="btn-neon" 
                onClick={() => handleVerifyBio(false)} 
                disabled={verifying}
              >
                {verifying ? "Verifying..." : "Verify Profile Bio"}
              </button>
              <button 
                className="btn-glass" 
                onClick={() => handleVerifyBio(true)} 
                disabled={verifying}
                style={{ borderColor: 'var(--neon-purple)' }}
              >
                Simulate Success (Testing)
              </button>
              <button 
                className="btn-glass" 
                onClick={handleResetVerification} 
                disabled={resetting}
              >
                Reset Application
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
