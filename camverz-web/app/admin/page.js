'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '../../lib/auth-context';
import {
  getAdminOverview,
  getAdminUsers,
  getAdminUserJourney,
  getAdminAdFlow,
  resetAdminTestUser,
} from '../../lib/api';
import {
  ShieldCheck,
  ShieldAlert,
  Smartphone,
  Play,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Search,
  Filter,
  RefreshCw,
  ChevronRight,
  X,
  Activity,
  Layers,
  MapPin,
  Calendar,
  Wifi,
  Eye,
  DollarSign,
  TrendingUp,
  UserCheck,
  UserX,
  Tv,
  ArrowRight,
  Sparkles,
  Trash2,
} from 'lucide-react';

const ADMIN_EMAIL = 'mohitjain1619@gmail.com';

export default function AdminPage() {
  const { user, loading: authLoading, signInWithGoogle } = useAuth();

  const [activeTab, setActiveTab] = useState('users'); // 'users' | 'adflow'
  const [overview, setOverview] = useState(null);
  const [loadingOverview, setLoadingOverview] = useState(true);

  // Users & Journeys state
  const [usersList, setUsersList] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [playStoreOnly, setPlayStoreOnly] = useState(false);
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Journey drawer state
  const [selectedJourney, setSelectedJourney] = useState(null);
  const [loadingJourney, setLoadingJourney] = useState(false);

  // Ad flow state
  const [adFlowEvents, setAdFlowEvents] = useState([]);
  const [loadingAdFlow, setLoadingAdFlow] = useState(false);
  const [adFormatFilter, setAdFormatFilter] = useState('ALL');
  const [adStatusFilter, setAdStatusFilter] = useState('ALL');
  const [adDateFilter, setAdDateFilter] = useState('');
  const [resettingUser, setResettingUser] = useState(false);

  // Live IST Clock
  const [currentISTTime, setCurrentISTTime] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(true);

  // Update IST clock every second
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentISTTime(
        now.toLocaleString('en-IN', {
          timeZone: 'Asia/Kolkata',
          day: '2-digit',
          month: 'short',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: true,
        }) + ' IST'
      );
    };
    updateTime();
    const timer = setInterval(updateTime, 1000);
    return () => clearInterval(timer);
  }, []);

  const isAuthorized = useMemo(() => {
    if (!user) return false;
    const email = (user.email || '').trim().toLowerCase();
    return email === ADMIN_EMAIL.toLowerCase();
  }, [user]);

  // Fetch overview stats
  const fetchOverview = useCallback(async () => {
    try {
      setLoadingOverview(true);
      const res = await getAdminOverview();
      if (res?.ok) {
        setOverview(res);
      }
    } catch (err) {
      console.error('Failed to fetch admin overview:', err);
    } finally {
      setLoadingOverview(false);
    }
  }, []);

  // Fetch users & devices
  const fetchUsers = useCallback(async () => {
    try {
      setLoadingUsers(true);
      const params = {};
      if (searchQuery.trim()) params.search = searchQuery.trim();
      if (playStoreOnly) params.playStoreOnly = 'true';
      if (statusFilter !== 'ALL') params.status = statusFilter;

      const res = await getAdminUsers(params);
      if (res?.ok) {
        setUsersList(res.users || []);
      }
    } catch (err) {
      console.error('Failed to fetch admin users:', err);
    } finally {
      setLoadingUsers(false);
    }
  }, [searchQuery, playStoreOnly, statusFilter]);

  // Fetch ad lifecycle flow
  const fetchAdFlow = useCallback(async () => {
    try {
      setLoadingAdFlow(true);
      const params = {};
      if (adFormatFilter !== 'ALL') params.format = adFormatFilter;
      if (adStatusFilter !== 'ALL') params.status = adStatusFilter;
      if (adDateFilter) params.date = adDateFilter;

      const res = await getAdminAdFlow(params);
      if (res?.ok) {
        setAdFlowEvents(res.events || []);
      }
    } catch (err) {
      console.error('Failed to fetch ad flow:', err);
    } finally {
      setLoadingAdFlow(false);
    }
  }, [adFormatFilter, adStatusFilter, adDateFilter]);

  // Fetch journey details for a user/device
  const openJourney = async (identifier) => {
    try {
      setLoadingJourney(true);
      setSelectedJourney(null);
      const res = await getAdminUserJourney(identifier);
      if (res?.ok) {
        setSelectedJourney(res);
      }
    } catch (err) {
      console.error('Failed to fetch user journey:', err);
    } finally {
      setLoadingJourney(false);
    }
  };

  const handleResetTestUser = async () => {
    if (!window.confirm("Delete mohitjain1619@gmail.com from database to test fresh onboarding flow?")) return;
    try {
      setResettingUser(true);
      const res = await resetAdminTestUser();
      alert(res?.message || 'User deleted successfully! Next sign in on phone will start fresh onboarding.');
      fetchOverview();
      fetchUsers();
    } catch (err) {
      alert('Error: ' + (err?.response?.data?.error || err.message));
    } finally {
      setResettingUser(false);
    }
  };

  // Initial fetch on authorization
  useEffect(() => {
    if (isAuthorized) {
      fetchOverview();
      fetchUsers();
      fetchAdFlow();
    }
  }, [isAuthorized, fetchOverview, fetchUsers, fetchAdFlow]);

  // Auto-refresh interval (15s)
  useEffect(() => {
    if (!isAuthorized || !autoRefresh) return;
    const interval = setInterval(() => {
      fetchOverview();
      if (activeTab === 'users') fetchUsers();
      if (activeTab === 'adflow') fetchAdFlow();
    }, 15000);
    return () => clearInterval(interval);
  }, [isAuthorized, autoRefresh, activeTab, fetchOverview, fetchUsers, fetchAdFlow]);

  const adminScopedStyles = (
    <style dangerouslySetInnerHTML={{__html: `
      .adm-wrapper {
        min-height: 100vh;
        background: radial-gradient(circle at 10% 10%, rgba(84, 214, 210, 0.08), transparent 40%),
                    radial-gradient(circle at 90% 20%, rgba(236, 72, 153, 0.08), transparent 40%),
                    #0b1115;
        color: #e2e8f0;
        font-family: var(--font-body, 'DM Sans', sans-serif);
        padding-top: 100px;
        padding-bottom: 80px;
        padding-left: 24px;
        padding-right: 24px;
      }
      .adm-container {
        max-width: 1320px;
        margin: 0 auto;
        display: flex;
        flex-direction: column;
        gap: 24px;
      }
      .adm-header-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 16px;
        padding: 20px 24px;
        background: rgba(18, 29, 34, 0.75);
        backdrop-filter: blur(20px);
        -webkit-backdrop-filter: blur(20px);
        border: 1px solid rgba(84, 214, 210, 0.2);
        border-radius: 20px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
      }
      .adm-title-group {
        display: flex;
        align-items: center;
        gap: 16px;
      }
      .adm-title-icon {
        width: 48px;
        height: 48px;
        border-radius: 14px;
        background: linear-gradient(135deg, #00e5ff 0%, #3b82f6 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        box-shadow: 0 0 20px rgba(0, 229, 255, 0.4);
      }
      .adm-title-text h1 {
        font-family: var(--font-display, 'Space Grotesk', sans-serif);
        font-size: 1.4rem;
        font-weight: 700;
        letter-spacing: 0.5px;
        color: #fff;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .adm-badge-live {
        font-size: 0.65rem;
        font-family: monospace;
        padding: 3px 8px;
        border-radius: 20px;
        background: rgba(16, 185, 129, 0.15);
        color: #34d399;
        border: 1px solid rgba(16, 185, 129, 0.3);
        display: inline-flex;
        align-items: center;
        gap: 5px;
      }
      .adm-badge-live-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #34d399;
        box-shadow: 0 0 8px #34d399;
      }
      .adm-subtext {
        font-size: 0.8rem;
        color: rgba(235, 246, 245, 0.6);
        margin-top: 4px;
      }
      .adm-controls {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
      .adm-clock {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 14px;
        background: rgba(0, 229, 255, 0.08);
        border: 1px solid rgba(0, 229, 255, 0.25);
        border-radius: 12px;
        font-family: monospace;
        font-size: 0.82rem;
        color: #54d6d2;
      }
      .adm-btn {
        padding: 8px 16px;
        border-radius: 12px;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        transition: all 0.2s ease;
      }
      .adm-btn-auto {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.15);
        color: #cbd5e1;
      }
      .adm-btn-auto.active {
        background: rgba(84, 214, 210, 0.15);
        border-color: rgba(84, 214, 210, 0.4);
        color: #54d6d2;
      }
      .adm-btn-refresh {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.15);
        color: #fff;
      }
      .adm-btn-refresh:hover {
        background: rgba(84, 214, 210, 0.2);
        border-color: rgba(84, 214, 210, 0.5);
      }
      .adm-kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
        gap: 16px;
      }
      .adm-card {
        background: rgba(18, 29, 34, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 18px;
        padding: 20px;
        position: relative;
        overflow: hidden;
        transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
      }
      .adm-card:hover {
        transform: translateY(-2px);
        border-color: rgba(84, 214, 210, 0.3);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
      }
      .adm-card-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
      }
      .adm-card-label {
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.8px;
        color: rgba(255, 255, 255, 0.55);
        font-weight: 600;
      }
      .adm-card-icon {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .adm-card-val-row {
        display: flex;
        align-items: baseline;
        gap: 8px;
      }
      .adm-card-val {
        font-size: 2.1rem;
        font-weight: 800;
        font-family: var(--font-display, 'Space Grotesk', monospace);
        color: #fff;
        line-height: 1;
      }
      .adm-card-subval {
        font-size: 0.85rem;
        font-family: monospace;
        color: rgba(255, 255, 255, 0.5);
      }
      .adm-card-footer {
        margin-top: 14px;
        font-size: 0.78rem;
        display: flex;
        align-items: center;
        gap: 6px;
      }
      .adm-funnel-box {
        background: rgba(18, 29, 34, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 18px;
        padding: 24px;
      }
      .adm-funnel-header {
        font-size: 0.85rem;
        text-transform: uppercase;
        letter-spacing: 0.8px;
        color: #54d6d2;
        font-weight: 700;
        margin-bottom: 18px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .adm-funnel-steps {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 12px;
      }
      .adm-funnel-step {
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid rgba(255, 255, 255, 0.06);
        border-radius: 14px;
        padding: 14px;
        text-align: center;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
      }
      .adm-step-title {
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.6);
        font-weight: 500;
      }
      .adm-step-num {
        font-size: 1.5rem;
        font-weight: 800;
        font-family: monospace;
        margin: 6px 0;
      }
      .adm-step-pct {
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.4);
      }
      .adm-tabs {
        display: flex;
        gap: 16px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        padding-bottom: 2px;
      }
      .adm-tab-btn {
        background: none;
        border: none;
        color: rgba(255, 255, 255, 0.55);
        font-size: 0.92rem;
        font-weight: 600;
        padding: 10px 16px;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        position: relative;
        transition: color 0.2s;
      }
      .adm-tab-btn:hover {
        color: #fff;
      }
      .adm-tab-btn.active {
        color: #54d6d2;
      }
      .adm-tab-btn.active::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        right: 0;
        height: 2px;
        background: #54d6d2;
        box-shadow: 0 0 10px #54d6d2;
      }
      .adm-filter-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 12px;
        background: rgba(18, 29, 34, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 16px;
        padding: 14px 18px;
      }
      .adm-search-input {
        background: rgba(0, 0, 0, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 10px;
        padding: 8px 14px 8px 36px;
        color: #fff;
        font-size: 0.82rem;
        width: 300px;
        max-width: 100%;
        outline: none;
      }
      .adm-search-input:focus {
        border-color: #54d6d2;
      }
      .adm-select {
        background: #0d161a;
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 10px;
        padding: 8px 14px;
        color: #e2e8f0;
        font-size: 0.82rem;
        outline: none;
      }
      .adm-table-wrap {
        background: rgba(18, 29, 34, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 18px;
        overflow: hidden;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
      }
      .adm-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.82rem;
        text-align: left;
      }
      .adm-table th {
        background: rgba(255, 255, 255, 0.03);
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        padding: 14px 18px;
        font-family: monospace;
        font-size: 0.72rem;
        letter-spacing: 0.5px;
        color: rgba(255, 255, 255, 0.5);
        text-transform: uppercase;
      }
      .adm-table td {
        padding: 14px 18px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.04);
        color: #e2e8f0;
      }
      .adm-table tr:hover td {
        background: rgba(255, 255, 255, 0.02);
      }
      .adm-pill {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 3px 10px;
        border-radius: 20px;
        font-size: 0.72rem;
        font-weight: 700;
        letter-spacing: 0.3px;
      }
      .adm-pill-play {
        background: rgba(16, 185, 129, 0.15);
        color: #34d399;
        border: 1px solid rgba(16, 185, 129, 0.3);
      }
      .adm-pill-apk {
        background: rgba(245, 158, 11, 0.15);
        color: #fbbf24;
        border: 1px solid rgba(245, 158, 11, 0.3);
      }
      .adm-pill-status {
        padding: 3px 8px;
        border-radius: 6px;
        font-size: 0.7rem;
        font-weight: 700;
      }
      .adm-pill-status.COMPLETED {
        background: rgba(16, 185, 129, 0.15);
        color: #34d399;
        border: 1px solid rgba(16, 185, 129, 0.3);
      }
      .adm-pill-status.DROPPED_OFF {
        background: rgba(245, 158, 11, 0.15);
        color: #fbbf24;
        border: 1px solid rgba(245, 158, 11, 0.3);
      }
      .adm-pill-status.HAS_ERRORS {
        background: rgba(239, 68, 68, 0.15);
        color: #f87171;
        border: 1px solid rgba(239, 68, 68, 0.3);
      }
      .adm-pill-status.IN_ONBOARDING {
        background: rgba(84, 214, 210, 0.15);
        color: #54d6d2;
        border: 1px solid rgba(84, 214, 210, 0.3);
      }
      .adm-btn-inspect {
        background: rgba(84, 214, 210, 0.1);
        border: 1px solid rgba(84, 214, 210, 0.3);
        color: #54d6d2;
        padding: 6px 12px;
        border-radius: 8px;
        font-size: 0.75rem;
        font-weight: 600;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        transition: all 0.2s;
      }
      .adm-btn-inspect:hover {
        background: rgba(84, 214, 210, 0.25);
        box-shadow: 0 0 12px rgba(84, 214, 210, 0.3);
      }
      .adm-modal-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.85);
        backdrop-filter: blur(8px);
        -webkit-backdrop-filter: blur(8px);
        z-index: 9999;
        display: flex;
        justify-content: flex-end;
      }
      .adm-drawer {
        width: 100%;
        max-width: 640px;
        height: 100%;
        background: #0d161a;
        border-left: 1px solid rgba(84, 214, 210, 0.25);
        overflow-y: auto;
        padding: 30px;
        display: flex;
        flex-direction: column;
        box-shadow: -15px 0 50px rgba(0, 0, 0, 0.7);
      }
      .adm-timeline-node {
        position: relative;
        padding-left: 28px;
        margin-bottom: 24px;
      }
      .adm-timeline-node::before {
        content: '';
        position: absolute;
        left: 0;
        top: 2px;
        bottom: -24px;
        width: 2px;
        background: rgba(255, 255, 255, 0.1);
      }
      .adm-timeline-node:last-child::before {
        display: none;
      }
      .adm-timeline-dot {
        position: absolute;
        left: -6px;
        top: 2px;
        width: 14px;
        height: 14px;
        border-radius: 50%;
        background: #34d399;
        border: 3px solid #0d161a;
      }
      .adm-timeline-dot.error {
        background: #f87171;
      }
      .adm-step-card {
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 12px;
        padding: 14px;
      }
    `}} />
  );

  // 1. Loading State
  if (authLoading) {
    return (
      <div className="adm-wrapper" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {adminScopedStyles}
        <div style={{ textAlign: 'center' }}>
          <RefreshCw className="animate-spin" style={{ width: 40, height: 40, color: '#54d6d2', margin: '0 auto 16px' }} />
          <p style={{ color: '#94a3b8', fontFamily: 'monospace' }}>Verifying Administrator Credentials...</p>
        </div>
      </div>
    );
  }

  // 2. Unauthorized State
  if (!isAuthorized) {
    return (
      <div className="adm-wrapper" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {adminScopedStyles}
        <div style={{
          maxWidth: 460,
          width: '100%',
          background: 'rgba(18, 29, 34, 0.9)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: 24,
          padding: 36,
          textAlign: 'center',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.6)'
        }}>
          <div style={{
            width: 64,
            height: 64,
            borderRadius: 18,
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px',
            color: '#f87171'
          }}>
            <ShieldAlert size={32} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', marginBottom: 8 }}>Restricted Access</h1>
          <p style={{ color: '#94a3b8', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: 20 }}>
            This monitoring hub is strictly restricted to administrator:
            <br />
            <span style={{
              display: 'inline-block',
              marginTop: 10,
              padding: '6px 12px',
              borderRadius: 8,
              background: 'rgba(239, 68, 68, 0.15)',
              color: '#fca5a5',
              fontFamily: 'monospace',
              fontWeight: 600
            }}>
              {ADMIN_EMAIL}
            </span>
          </p>
          {user && (
            <div style={{
              padding: '10px 14px',
              background: 'rgba(255, 255, 255, 0.05)',
              borderRadius: 10,
              fontSize: '0.8rem',
              color: '#94a3b8',
              marginBottom: 20
            }}>
              Current: <strong style={{ color: '#fff' }}>{user.email || 'Anonymous'}</strong>
            </div>
          )}
          <button
            onClick={() => signInWithGoogle()}
            style={{
              width: '100%',
              padding: '12px 24px',
              borderRadius: 12,
              background: 'linear-gradient(135deg, #00e5ff 0%, #3b82f6 100%)',
              color: '#fff',
              fontWeight: 600,
              border: 'none',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              boxShadow: '0 0 20px rgba(0, 229, 255, 0.3)'
            }}
          >
            <ShieldCheck size={18} />
            Sign in as Administrator
          </button>
        </div>
      </div>
    );
  }

  // 3. Authorized Dashboard
  return (
    <div className="adm-wrapper">
      {adminScopedStyles}

      <div className="adm-container">
        {/* Header Bar */}
        <div className="adm-header-bar">
          <div className="adm-title-group">
            <div className="adm-title-icon">
              <Activity size={24} />
            </div>
            <div className="adm-title-text">
              <h1>
                CAMVERZ TELEMETRY HUB
                <span className="adm-badge-live">
                  <span className="adm-badge-live-dot" />
                  LIVE PRODUCTION
                </span>
              </h1>
              <div className="adm-subtext">
                Admin: <strong style={{ color: '#54d6d2' }}>{ADMIN_EMAIL}</strong>
              </div>
            </div>
          </div>

          <div className="adm-controls">
            {/* Live IST Clock */}
            <div className="adm-clock">
              <Clock size={15} style={{ color: '#54d6d2' }} />
              <span>{currentISTTime || 'Loading IST...'}</span>
            </div>

            {/* Auto Refresh Toggle */}
            <button
              onClick={() => setAutoRefresh(!autoRefresh)}
              className={`adm-btn adm-btn-auto ${autoRefresh ? 'active' : ''}`}
            >
              <RefreshCw size={14} className={autoRefresh ? 'animate-spin' : ''} />
              Auto (15s)
            </button>

            {/* Manual Refresh Button */}
            <button
              onClick={() => {
                fetchOverview();
                if (activeTab === 'users') fetchUsers();
                if (activeTab === 'adflow') fetchAdFlow();
              }}
              className="adm-btn adm-btn-refresh"
              title="Refresh Data Now"
            >
              <RefreshCw size={14} />
              Refresh
            </button>

            {/* Reset Test User Button */}
            <button
              onClick={handleResetTestUser}
              disabled={resettingUser}
              className="adm-btn"
              style={{
                background: 'rgba(239, 68, 68, 0.15)',
                border: '1px solid rgba(239, 68, 68, 0.35)',
                color: '#f87171',
                cursor: resettingUser ? 'not-allowed' : 'pointer'
              }}
              title="Delete mohitjain1619@gmail.com from database to test fresh onboarding"
            >
              <Trash2 size={14} className={resettingUser ? 'animate-spin' : ''} />
              {resettingUser ? 'Resetting...' : 'Reset Test Account'}
            </button>
          </div>
        </div>

        {/* Top 4 KPI Cards */}
        <div className="adm-kpi-grid">
          {/* Card 1: Play Store Installs */}
          <div className="adm-card">
            <div className="adm-card-top">
              <span className="adm-card-label">Play Store Installs</span>
              <div className="adm-card-icon" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34d399' }}>
                <Play size={18} fill="#34d399" />
              </div>
            </div>
            <div className="adm-card-val-row">
              <span className="adm-card-val" style={{ color: '#34d399' }}>
                {overview?.totals?.play_store_installs ?? 0}
              </span>
              <span className="adm-card-subval">
                / {overview?.totals?.total_installs ?? 0} total
              </span>
            </div>
            <div className="adm-card-footer" style={{ color: '#34d399' }}>
              <span>🟢 Verified Google Play Downloads</span>
            </div>
          </div>

          {/* Card 2: Funnel Conversion */}
          <div className="adm-card">
            <div className="adm-card-top">
              <span className="adm-card-label">Funnel Conversion</span>
              <div className="adm-card-icon" style={{ background: 'rgba(84, 214, 210, 0.15)', color: '#54d6d2' }}>
                <UserCheck size={18} />
              </div>
            </div>
            <div className="adm-card-val-row">
              <span className="adm-card-val" style={{ color: '#54d6d2' }}>
                {overview?.funnel?.completed ?? 0}
              </span>
              <span className="adm-card-subval" style={{ color: '#54d6d2' }}>
                ({overview?.funnel?.completion_rate ?? 0}%)
              </span>
            </div>
            <div className="adm-card-footer" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>
              <span>From {overview?.funnel?.app_opens ?? 0} app launches</span>
            </div>
          </div>

          {/* Card 3: Active Devices */}
          <div className="adm-card">
            <div className="adm-card-top">
              <span className="adm-card-label">Active Devices (30m)</span>
              <div className="adm-card-icon" style={{ background: 'rgba(167, 139, 250, 0.15)', color: '#a78bfa' }}>
                <Smartphone size={18} />
              </div>
            </div>
            <div className="adm-card-val-row">
              <span className="adm-card-val" style={{ color: '#a78bfa' }}>
                {overview?.active_devices_last_30m ?? 0}
              </span>
              <span className="adm-card-subval" style={{ color: '#34d399', display: 'flex', alignItems: 'center', gap: 4 }}>
                ● Online
              </span>
            </div>
            <div className="adm-card-footer" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>
              <span>Total Tracked: {overview?.totals?.total_devices ?? 0} devices</span>
            </div>
          </div>

          {/* Card 4: Ad Impressions & Revenue */}
          <div className="adm-card">
            <div className="adm-card-top">
              <span className="adm-card-label">Ad Impressions</span>
              <div className="adm-card-icon" style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24' }}>
                <DollarSign size={18} />
              </div>
            </div>
            <div className="adm-card-val-row">
              <span className="adm-card-val" style={{ color: '#fbbf24' }}>
                {overview?.ads?.delivered ?? 0}
              </span>
              <span className="adm-card-subval" style={{ color: '#f59e0b' }}>
                ${(overview?.ads?.total_revenue_usd ?? 0).toFixed(4)} USD
              </span>
            </div>
            <div className="adm-card-footer" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>
              <span>Rewarded: {overview?.ads?.rewarded_count ?? 0} | Interstitial: {overview?.ads?.interstitial_count ?? 0}</span>
            </div>
          </div>
        </div>

        {/* Funnel Progress Section */}
        {overview?.funnel && (
          <div className="adm-funnel-box">
            <div className="adm-funnel-header">
              <TrendingUp size={16} />
              Step-by-Step Onboarding Funnel Progress
            </div>
            <div className="adm-funnel-steps">
              {[
                { label: '1. App Opened', count: overview.funnel.app_opens, color: '#60a5fa' },
                { label: '2. Google Sign-In', count: overview.funnel.auth_success, color: '#54d6d2' },
                { label: '3. Gender Set', count: overview.funnel.gender_selected, color: '#a78bfa' },
                { label: '4. DOB Set', count: overview.funnel.dob_selected, color: '#c084fc' },
                { label: '5. Location OK', count: overview.funnel.location_resolved, color: '#f472b6' },
                { label: '6. Completed', count: overview.funnel.completed, color: '#34d399' },
              ].map((step, idx) => (
                <div key={idx} className="adm-funnel-step">
                  <div className="adm-step-title">{step.label}</div>
                  <div className="adm-step-num" style={{ color: step.color }}>{step.count ?? 0}</div>
                  <div className="adm-step-pct">
                    {overview.funnel.app_opens > 0
                      ? `${Math.round(((step.count ?? 0) / overview.funnel.app_opens) * 100)}%`
                      : '0%'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Navigation Tabs */}
        <div className="adm-tabs">
          <button
            onClick={() => setActiveTab('users')}
            className={`adm-tab-btn ${activeTab === 'users' ? 'active' : ''}`}
          >
            <Smartphone size={16} />
            Users & Onboarding Journeys
          </button>
          <button
            onClick={() => setActiveTab('adflow')}
            className={`adm-tab-btn ${activeTab === 'adflow' ? 'active' : ''}`}
          >
            <Tv size={16} />
            Date-wise Ad Lifecycle Inspector
          </button>
        </div>

        {/* TAB 1: USERS & JOURNEYS */}
        {activeTab === 'users' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Filter Bar */}
            <div className="adm-filter-bar">
              <div style={{ position: 'relative' }}>
                <Search size={15} style={{ position: 'absolute', left: 12, top: 11, color: '#94a3b8' }} />
                <input
                  type="text"
                  placeholder="Search by Email or Device ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="adm-search-input"
                />
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                {/* Play Store Toggle */}
                <button
                  onClick={() => setPlayStoreOnly(!playStoreOnly)}
                  className={`adm-btn ${playStoreOnly ? 'adm-pill-play' : 'adm-btn-auto'}`}
                  style={{ border: '1px solid' }}
                >
                  <Play size={14} fill={playStoreOnly ? '#34d399' : 'none'} />
                  Play Store Only
                </button>

                {/* Status Dropdown */}
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="adm-select"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="IN_ONBOARDING">In Onboarding</option>
                  <option value="DROPPED_OFF">Dropped Off</option>
                  <option value="HAS_ERRORS">Has Errors</option>
                </select>

                <button
                  onClick={fetchUsers}
                  className="adm-btn adm-btn-refresh"
                  style={{ background: 'rgba(84, 214, 210, 0.15)', borderColor: 'rgba(84, 214, 210, 0.4)', color: '#54d6d2' }}
                >
                  <Filter size={14} />
                  Apply
                </button>
              </div>
            </div>

            {/* Users Table */}
            <div className="adm-table-wrap">
              <div style={{ overflowX: 'auto' }}>
                <table className="adm-table">
                  <thead>
                    <tr>
                      <th>USER / DEVICE ID</th>
                      <th>SOURCE</th>
                      <th>DEVICE & OS</th>
                      <th>LOCATION / NET</th>
                      <th>STATUS</th>
                      <th>LAST SEEN (IST)</th>
                      <th style={{ textAlign: 'right' }}>ACTION</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loadingUsers ? (
                      <tr>
                        <td colSpan="7" style={{ textAlign: 'center', padding: '50px 20px', color: '#94a3b8' }}>
                          <RefreshCw className="animate-spin" size={24} style={{ margin: '0 auto 10px', color: '#54d6d2' }} />
                          Loading tracked users...
                        </td>
                      </tr>
                    ) : usersList.length === 0 ? (
                      <tr>
                        <td colSpan="7" style={{ textAlign: 'center', padding: '50px 20px', color: '#94a3b8', fontFamily: 'monospace' }}>
                          No users or devices recorded yet. (Launch the app to see events appear in real-time)
                        </td>
                      </tr>
                    ) : (
                      usersList.map((item, idx) => (
                        <tr key={idx}>
                          <td>
                            {item.user_email ? (
                              <div>
                                <strong style={{ color: '#fff' }}>{item.user_name || item.user_email}</strong>
                                <div style={{ fontSize: '0.72rem', color: '#94a3b8', fontFamily: 'monospace' }}>{item.user_email}</div>
                              </div>
                            ) : (
                              <div>
                                <span style={{ fontFamily: 'monospace', color: '#cbd5e1' }}>Device {item.device_id?.substring(0, 12)}...</span>
                                <div style={{ fontSize: '0.7rem', color: '#f59e0b' }}>Pre-login visitor</div>
                              </div>
                            )}
                          </td>
                          <td>
                            {item.is_play_store ? (
                              <span className="adm-pill adm-pill-play">
                                <Play size={10} fill="#34d399" />
                                Google Play
                              </span>
                            ) : (
                              <span className="adm-pill adm-pill-apk">
                                Direct APK
                              </span>
                            )}
                          </td>
                          <td>
                            <div style={{ color: '#fff', fontWeight: 600 }}>{item.device_model || 'Unknown'}</div>
                            <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{item.os_version || 'Android'}</div>
                          </td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: '#e2e8f0' }}>
                              <MapPin size={12} style={{ color: '#54d6d2' }} />
                              {item.country || 'IN'}
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: '0.72rem', color: '#64748b' }}>
                              <Wifi size={12} />
                              {item.network_type || 'WiFi'}
                            </div>
                          </td>
                          <td>
                            <span className={`adm-pill-status ${item.status}`}>
                              {item.status}
                            </span>
                          </td>
                          <td style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: '#94a3b8' }}>
                            {item.last_seen_ist}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button
                              onClick={() => openJourney(item.user_email || item.device_id)}
                              className="adm-btn-inspect"
                            >
                              Inspect Journey
                              <ChevronRight size={14} />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: AD LIFECYCLE FLOW */}
        {activeTab === 'adflow' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Filter Bar */}
            <div className="adm-filter-bar">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                <select
                  value={adFormatFilter}
                  onChange={(e) => setAdFormatFilter(e.target.value)}
                  className="adm-select"
                >
                  <option value="ALL">All Formats</option>
                  <option value="rewarded">Rewarded Video</option>
                  <option value="interstitial">Interstitial</option>
                </select>

                <select
                  value={adStatusFilter}
                  onChange={(e) => setAdStatusFilter(e.target.value)}
                  className="adm-select"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="DELIVERED">Delivered</option>
                  <option value="IMPRESSION">Impression</option>
                  <option value="REWARDED">Rewarded</option>
                  <option value="FAILED">Failed</option>
                </select>

                <input
                  type="date"
                  value={adDateFilter}
                  onChange={(e) => setAdDateFilter(e.target.value)}
                  className="adm-select"
                  style={{ fontFamily: 'monospace' }}
                />

                <button
                  onClick={fetchAdFlow}
                  className="adm-btn adm-btn-refresh"
                  style={{ background: 'rgba(84, 214, 210, 0.15)', borderColor: 'rgba(84, 214, 210, 0.4)', color: '#54d6d2' }}
                >
                  <Filter size={14} />
                  Filter Ads
                </button>
              </div>

              <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontFamily: 'monospace' }}>
                Total: {adFlowEvents.length} events
              </div>
            </div>

            {/* Ad Flow Table */}
            <div className="adm-table-wrap">
              <div style={{ overflowX: 'auto' }}>
                <table className="adm-table">
                  <thead>
                    <tr>
                      <th>TIME (IST)</th>
                      <th>USER / DEVICE</th>
                      <th>TRIGGER / PLACEMENT</th>
                      <th>FORMAT</th>
                      <th>WINNER NETWORK</th>
                      <th>REVENUE / ECPM</th>
                      <th>STATUS</th>
                      <th>ERROR REASON</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loadingAdFlow ? (
                      <tr>
                        <td colSpan="8" style={{ textAlign: 'center', padding: '50px 20px', color: '#94a3b8' }}>
                          <RefreshCw className="animate-spin" size={24} style={{ margin: '0 auto 10px', color: '#54d6d2' }} />
                          Loading ad flow...
                        </td>
                      </tr>
                    ) : adFlowEvents.length === 0 ? (
                      <tr>
                        <td colSpan="8" style={{ textAlign: 'center', padding: '50px 20px', color: '#94a3b8', fontFamily: 'monospace' }}>
                          No ad lifecycle events matching filter.
                        </td>
                      </tr>
                    ) : (
                      adFlowEvents.map((ad, idx) => (
                        <tr key={idx}>
                          <td style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: '#cbd5e1', whiteSpace: 'nowrap' }}>
                            {ad.timestamp_ist}
                          </td>
                          <td>
                            <strong style={{ color: '#fff' }}>{ad.user_email || 'Anonymous'}</strong>
                            <div style={{ fontSize: '0.7rem', color: '#64748b' }}>{ad.country || 'IN'}</div>
                          </td>
                          <td>
                            <span style={{
                              padding: '2px 8px',
                              borderRadius: 6,
                              background: 'rgba(255, 255, 255, 0.05)',
                              border: '1px solid rgba(255, 255, 255, 0.1)',
                              color: '#54d6d2',
                              fontFamily: 'monospace',
                              fontSize: '0.75rem'
                            }}>
                              {ad.placement}
                            </span>
                            {ad.trigger_reason && ad.trigger_reason !== 'unspecified' && (
                              <div style={{ fontSize: '0.7rem', color: '#64748b', marginTop: 2 }}>
                                why: {ad.trigger_reason}
                              </div>
                            )}
                          </td>
                          <td style={{ textTransform: 'capitalize', fontWeight: 600, color: '#fff' }}>
                            {ad.ad_type}
                          </td>
                          <td style={{ textTransform: 'capitalize', color: '#34d399', fontWeight: 700 }}>
                            {ad.ad_network}
                          </td>
                          <td style={{ fontFamily: 'monospace' }}>
                            <div style={{ color: '#fbbf24', fontWeight: 700 }}>${(ad.revenue || 0).toFixed(6)}</div>
                            <div style={{ fontSize: '0.7rem', color: '#64748b' }}>eCPM: ${(ad.ecpm || 0).toFixed(2)}</div>
                          </td>
                          <td>
                            <span className={`adm-pill-status ${ad.status}`}>
                              {ad.status}
                            </span>
                          </td>
                          <td style={{ color: '#f87171', fontSize: '0.75rem' }}>
                            {ad.error_message || '—'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* USER JOURNEY MODAL / DRAWER */}
      {selectedJourney && (
        <div className="adm-modal-overlay" onClick={() => setSelectedJourney(null)}>
          <div className="adm-drawer" onClick={(e) => e.stopPropagation()}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: 16, borderBottom: '1px solid rgba(255, 255, 255, 0.1)', marginBottom: 20 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ width: 38, height: 38, borderRadius: 10, background: 'rgba(84, 214, 210, 0.15)', color: '#54d6d2', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Smartphone size={20} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700, color: '#fff' }}>User Journey Inspector</h3>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8', fontFamily: 'monospace' }}>
                    {selectedJourney.user?.email || selectedJourney.user?.device_id}
                  </div>
                </div>
              </div>
              <button
                onClick={() => setSelectedJourney(null)}
                style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: 4 }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Device Info Card */}
            <div style={{
              background: 'rgba(255, 255, 255, 0.03)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              borderRadius: 14,
              padding: 16,
              marginBottom: 24,
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: 12,
              fontSize: '0.8rem'
            }}>
              <div>
                <span style={{ color: '#64748b' }}>Device:</span>
                <div style={{ color: '#fff', fontWeight: 600 }}>{selectedJourney.user?.device_model || 'Unknown'}</div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>OS:</span>
                <div style={{ color: '#fff', fontWeight: 600 }}>{selectedJourney.user?.os_version || 'Android'}</div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>Source:</span>
                <div style={{ color: selectedJourney.user?.is_play_store ? '#34d399' : '#fbbf24', fontWeight: 700 }}>
                  {selectedJourney.user?.is_play_store ? '🟢 Google Play Store' : 'Direct APK'}
                </div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>Country / Net:</span>
                <div style={{ color: '#fff', fontWeight: 600 }}>
                  {selectedJourney.user?.country || 'IN'} ({selectedJourney.user?.network_type || 'WiFi'})
                </div>
              </div>
            </div>

            {/* Timeline */}
            <div style={{ flex: 1 }}>
              <h4 style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.5px', color: '#54d6d2', fontFamily: 'monospace', marginBottom: 18 }}>
                Step-by-Step Chronological Trail (IST)
              </h4>

              {loadingJourney ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: '#94a3b8' }}>
                  <RefreshCw className="animate-spin" size={24} style={{ margin: '0 auto 10px', color: '#54d6d2' }} />
                  Loading journey steps...
                </div>
              ) : (selectedJourney.journey || []).length === 0 ? (
                <p style={{ color: '#64748b', fontSize: '0.85rem' }}>No detailed journey steps logged for this session yet.</p>
              ) : (
                (selectedJourney.journey || []).map((step, idx) => (
                  <div key={idx} className="adm-timeline-node">
                    <div className={`adm-timeline-dot ${step.is_success ? '' : 'error'}`} />
                    <div className="adm-step-card">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                        <strong style={{ fontSize: '0.85rem', color: '#fff', fontFamily: 'monospace' }}>
                          {step.step_name}
                        </strong>
                        <span style={{ fontSize: '0.72rem', color: '#54d6d2', fontFamily: 'monospace' }}>
                          {step.timestamp_ist}
                        </span>
                      </div>

                      {step.elapsed_sec > 0 && (
                        <div style={{ fontSize: '0.72rem', color: '#94a3b8', fontFamily: 'monospace', marginBottom: 6 }}>
                          ⏱️ +{step.elapsed_sec}s since previous action
                        </div>
                      )}

                      {step.duration_seconds > 0 && (
                        <div style={{ fontSize: '0.75rem', color: '#fbbf24', fontFamily: 'monospace', marginBottom: 6 }}>
                          ⌛ Spent {step.duration_seconds}s on screen
                        </div>
                      )}

                      {step.error_message && (
                        <div style={{
                          fontSize: '0.75rem',
                          color: '#fca5a5',
                          background: 'rgba(239, 68, 68, 0.15)',
                          border: '1px solid rgba(239, 68, 68, 0.3)',
                          padding: '6px 10px',
                          borderRadius: 8,
                          marginBottom: 6,
                          fontFamily: 'monospace'
                        }}>
                          ⚠️ {step.error_message}
                        </div>
                      )}

                      {step.details && Object.keys(step.details).length > 0 && (
                        <div style={{
                          fontSize: '0.72rem',
                          color: '#94a3b8',
                          background: 'rgba(0, 0, 0, 0.3)',
                          padding: 8,
                          borderRadius: 8,
                          fontFamily: 'monospace',
                          overflowX: 'auto'
                        }}>
                          {JSON.stringify(step.details, null, 2)}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
