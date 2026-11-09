'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '../../lib/auth-context';
import {
  getAdminOverview,
  getAdminUsers,
  getAdminUserJourney,
  getAdminAdFlow,
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
} from 'lucide-react';

const ADMIN_EMAIL = 'mohitjain1619@gmail.com';

export default function AdminPage() {
  const { user, loading: authLoading, signInWithGoogle } = useAuth();

  const [activeTab, setActiveTab] = useState('users'); // 'users' | 'adflow' | 'errors'
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

  // 1. Loading State
  if (authLoading) {
    return (
      <div className="min-h-screen bg-[#07090E] text-white flex flex-col items-center justify-center p-6">
        <RefreshCw className="w-10 h-10 text-cyan-400 animate-spin mb-4" />
        <p className="text-gray-400 font-mono text-sm tracking-wider">Verifying Admin Credentials...</p>
      </div>
    );
  }

  // 2. Unauthorized State
  if (!isAuthorized) {
    return (
      <div className="min-h-screen bg-[#07090E] text-white flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-[#0D121D] border border-red-500/30 rounded-2xl p-8 text-center shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-red-600 via-rose-500 to-amber-500" />
          <div className="w-16 h-16 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-center justify-center mx-auto mb-5 text-red-400">
            <ShieldAlert className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold text-white mb-2">Restricted Access</h1>
          <p className="text-gray-400 text-sm leading-relaxed mb-6">
            This monitoring center is strictly restricted to administrator:
            <br />
            <span className="font-mono text-red-300 font-semibold bg-red-950/40 px-2 py-1 rounded inline-block mt-2">
              {ADMIN_EMAIL}
            </span>
          </p>
          {user ? (
            <div className="p-3 bg-white/5 rounded-xl border border-white/10 mb-6 text-xs text-gray-400">
              Signed in as: <span className="text-white font-medium">{user.email || 'Anonymous'}</span>
            </div>
          ) : null}
          <button
            onClick={() => signInWithGoogle()}
            className="w-full py-3 px-6 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-medium shadow-lg shadow-cyan-500/20 transition-all flex items-center justify-center gap-2"
          >
            <ShieldCheck className="w-4 h-4" />
            Sign in as Administrator
          </button>
        </div>
      </div>
    );
  }

  // 3. Authorized Admin Portal
  return (
    <div className="min-h-screen bg-[#07090E] text-gray-200">
      {/* Top Header */}
      <header className="border-b border-white/10 bg-[#0B0F17]/90 backdrop-blur-md sticky top-0 z-30 px-6 py-4">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white shadow-lg shadow-cyan-500/25">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold text-white tracking-wide">CAMVERZ TELEMETRY HUB</h1>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  LIVE PRODUCTION
                </span>
              </div>
              <p className="text-xs text-gray-400 font-mono">
                Admin: <span className="text-cyan-400">{ADMIN_EMAIL}</span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3 self-end md:self-auto">
            {/* IST Clock */}
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-xs font-mono text-cyan-300">
              <Clock className="w-3.5 h-3.5 text-cyan-400" />
              <span>{currentISTTime || 'Loading IST...'}</span>
            </div>

            {/* Auto Refresh Toggle */}
            <button
              onClick={() => setAutoRefresh(!autoRefresh)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all flex items-center gap-1.5 ${
                autoRefresh
                  ? 'bg-cyan-500/10 border-cyan-500/30 text-cyan-300'
                  : 'bg-white/5 border-white/10 text-gray-400'
              }`}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${autoRefresh ? 'animate-spin' : ''}`} />
              Auto (15s)
            </button>

            {/* Manual Refresh Button */}
            <button
              onClick={() => {
                fetchOverview();
                if (activeTab === 'users') fetchUsers();
                if (activeTab === 'adflow') fetchAdFlow();
              }}
              className="p-2 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 transition-all"
              title="Refresh Data Now"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-6 py-8 space-y-8">
        {/* KPI Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Card 1: Play Store Installs */}
          <div className="bg-[#0D121D] border border-white/10 rounded-2xl p-5 relative overflow-hidden group hover:border-emerald-500/30 transition-all">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">Play Store Installs</span>
              <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center border border-emerald-500/20">
                <Play className="w-4 h-4 fill-emerald-400" />
              </div>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-white font-mono">
                {overview?.totals?.play_store_installs ?? 0}
              </span>
              <span className="text-xs text-gray-400 font-mono">
                / {overview?.totals?.total_installs ?? 0} total
              </span>
            </div>
            <div className="mt-3 text-xs text-emerald-400 flex items-center gap-1">
              <span>🟢 Verified Google Play Downloads</span>
            </div>
          </div>

          {/* Card 2: Onboarding Funnel Completion */}
          <div className="bg-[#0D121D] border border-white/10 rounded-2xl p-5 relative overflow-hidden group hover:border-cyan-500/30 transition-all">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">Funnel Conversion</span>
              <div className="w-8 h-8 rounded-lg bg-cyan-500/10 text-cyan-400 flex items-center justify-center border border-cyan-500/20">
                <UserCheck className="w-4 h-4" />
              </div>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-white font-mono">
                {overview?.funnel?.completed ?? 0}
              </span>
              <span className="text-xs text-cyan-300 font-mono">
                ({overview?.funnel?.completion_rate ?? 0}%)
              </span>
            </div>
            <div className="mt-3 text-xs text-gray-400">
              From {overview?.funnel?.app_opens ?? 0} app launches
            </div>
          </div>

          {/* Card 3: Active Devices (Last 30m) */}
          <div className="bg-[#0D121D] border border-white/10 rounded-2xl p-5 relative overflow-hidden group hover:border-indigo-500/30 transition-all">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">Active Devices (30m)</span>
              <div className="w-8 h-8 rounded-lg bg-indigo-500/10 text-indigo-400 flex items-center justify-center border border-indigo-500/20">
                <Smartphone className="w-4 h-4" />
              </div>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-white font-mono">
                {overview?.active_devices_last_30m ?? 0}
              </span>
              <span className="text-xs text-emerald-400 font-mono flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
                Online
              </span>
            </div>
            <div className="mt-3 text-xs text-gray-400 font-mono">
              Total Tracked: {overview?.totals?.total_devices ?? 0} devices
            </div>
          </div>

          {/* Card 4: Ad Impressions & Revenue */}
          <div className="bg-[#0D121D] border border-white/10 rounded-2xl p-5 relative overflow-hidden group hover:border-amber-500/30 transition-all">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">Ad Impressions</span>
              <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center border border-amber-500/20">
                <DollarSign className="w-4 h-4" />
              </div>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-white font-mono">
                {overview?.ads?.delivered ?? 0}
              </span>
              <span className="text-xs text-amber-300 font-mono">
                ${(overview?.ads?.total_revenue_usd ?? 0).toFixed(4)} USD
              </span>
            </div>
            <div className="mt-3 text-xs text-gray-400">
              Rewarded: {overview?.ads?.rewarded_count ?? 0} | Interstitial: {overview?.ads?.interstitial_count ?? 0}
            </div>
          </div>
        </div>

        {/* Funnel Visual Progression Bar */}
        {overview?.funnel ? (
          <div className="bg-[#0D121D] border border-white/10 rounded-2xl p-6">
            <h2 className="text-sm font-semibold text-white uppercase tracking-wider mb-4 flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-cyan-400" />
              Step-by-Step Onboarding Funnel
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-6 gap-3 text-center">
              {[
                { label: '1. App Opened', count: overview.funnel.app_opens, color: 'text-blue-400' },
                { label: '2. Google Sign-In', count: overview.funnel.auth_success, color: 'text-cyan-400' },
                { label: '3. Gender Set', count: overview.funnel.gender_selected, color: 'text-indigo-400' },
                { label: '4. DOB Set', count: overview.funnel.dob_selected, color: 'text-purple-400' },
                { label: '5. Location OK', count: overview.funnel.location_resolved, color: 'text-pink-400' },
                { label: '6. Completed', count: overview.funnel.completed, color: 'text-emerald-400' },
              ].map((step, idx) => (
                <div key={idx} className="p-3 bg-white/5 rounded-xl border border-white/5 flex flex-col justify-between">
                  <div className="text-[11px] text-gray-400 font-medium">{step.label}</div>
                  <div className={`text-xl font-bold font-mono my-1 ${step.color}`}>{step.count ?? 0}</div>
                  <div className="text-[10px] text-gray-500">
                    {overview.funnel.app_opens > 0
                      ? `${Math.round(((step.count ?? 0) / overview.funnel.app_opens) * 100)}%`
                      : '0%'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {/* Tab Navigation */}
        <div className="flex border-b border-white/10 gap-6">
          <button
            onClick={() => setActiveTab('users')}
            className={`pb-3 font-medium text-sm flex items-center gap-2 transition-all relative ${
              activeTab === 'users'
                ? 'text-cyan-400 font-bold border-b-2 border-cyan-400'
                : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            Users & Onboarding Journeys
          </button>
          <button
            onClick={() => setActiveTab('adflow')}
            className={`pb-3 font-medium text-sm flex items-center gap-2 transition-all relative ${
              activeTab === 'adflow'
                ? 'text-cyan-400 font-bold border-b-2 border-cyan-400'
                : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            <Tv className="w-4 h-4" />
            Date-wise Ad Lifecycle Inspector
          </button>
        </div>

        {/* TAB 1: USERS & JOURNEYS */}
        {activeTab === 'users' && (
          <div className="space-y-4">
            {/* Filter Bar */}
            <div className="flex flex-col sm:flex-row gap-3 items-center justify-between bg-[#0D121D] p-4 rounded-2xl border border-white/10">
              <div className="relative w-full sm:w-80">
                <Search className="w-4 h-4 absolute left-3 top-3 text-gray-500" />
                <input
                  type="text"
                  placeholder="Search by Email or Device ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 bg-white/5 border border-white/10 rounded-xl text-xs text-white placeholder-gray-500 focus:outline-none focus:border-cyan-500"
                />
              </div>

              <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
                {/* Play Store Only Filter */}
                <button
                  onClick={() => setPlayStoreOnly(!playStoreOnly)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-medium border flex items-center gap-1.5 transition-all ${
                    playStoreOnly
                      ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300'
                      : 'bg-white/5 border-white/10 text-gray-400 hover:text-white'
                  }`}
                >
                  <Play className="w-3.5 h-3.5 fill-current" />
                  Play Store Only
                </button>

                {/* Status Dropdown */}
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="px-3 py-1.5 bg-[#0B0F17] border border-white/10 rounded-xl text-xs text-gray-300 focus:outline-none focus:border-cyan-500"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="IN_ONBOARDING">In Onboarding</option>
                  <option value="DROPPED_OFF">Dropped Off</option>
                  <option value="HAS_ERRORS">Has Errors</option>
                </select>

                <button
                  onClick={fetchUsers}
                  className="px-3 py-1.5 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 hover:bg-cyan-500/20 text-xs font-medium flex items-center gap-1"
                >
                  <Filter className="w-3.5 h-3.5" />
                  Apply
                </button>
              </div>
            </div>

            {/* Users Table */}
            <div className="bg-[#0D121D] border border-white/10 rounded-2xl overflow-hidden shadow-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-white/10 bg-white/[0.02] text-gray-400 font-mono">
                      <th className="py-3 px-4 font-medium">USER / DEVICE ID</th>
                      <th className="py-3 px-4 font-medium">SOURCE</th>
                      <th className="py-3 px-4 font-medium">DEVICE & OS</th>
                      <th className="py-3 px-4 font-medium">LOCATION / NET</th>
                      <th className="py-3 px-4 font-medium">STATUS</th>
                      <th className="py-3 px-4 font-medium">LAST SEEN (IST)</th>
                      <th className="py-3 px-4 font-medium text-right">ACTION</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {loadingUsers ? (
                      <tr>
                        <td colSpan="7" className="text-center py-12 text-gray-500">
                          <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-cyan-400" />
                          Loading tracked users...
                        </td>
                      </tr>
                    ) : usersList.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="text-center py-12 text-gray-500 font-mono">
                          No users or devices recorded yet.
                        </td>
                      </tr>
                    ) : (
                      usersList.map((item, idx) => (
                        <tr key={idx} className="hover:bg-white/[0.02] transition-colors">
                          {/* User / Device */}
                          <td className="py-3 px-4">
                            {item.user_email ? (
                              <div>
                                <div className="font-semibold text-white">{item.user_name || item.user_email}</div>
                                <div className="text-[11px] text-gray-400 font-mono">{item.user_email}</div>
                              </div>
                            ) : (
                              <div>
                                <span className="font-mono text-gray-300">Device {item.device_id.substring(0, 12)}...</span>
                                <div className="text-[10px] text-amber-400 font-sans">Pre-login visitor</div>
                              </div>
                            )}
                          </td>

                          {/* Source */}
                          <td className="py-3 px-4">
                            {item.is_play_store ? (
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                <Play className="w-3 h-3 fill-emerald-400" />
                                Google Play
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
                                Direct APK
                              </span>
                            )}
                          </td>

                          {/* Device & OS */}
                          <td className="py-3 px-4">
                            <div className="text-white font-medium">{item.device_model || 'Unknown'}</div>
                            <div className="text-[10px] text-gray-500">{item.os_version || 'Android'}</div>
                          </td>

                          {/* Location & Network */}
                          <td className="py-3 px-4">
                            <div className="text-gray-300 flex items-center gap-1">
                              <MapPin className="w-3 h-3 text-cyan-400" />
                              {item.country || 'IN'}
                            </div>
                            <div className="text-[10px] text-gray-500 flex items-center gap-1">
                              <Wifi className="w-3 h-3" />
                              {item.network_type || 'WiFi'}
                            </div>
                          </td>

                          {/* Status */}
                          <td className="py-3 px-4">
                            <span
                              className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                                item.status === 'COMPLETED'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : item.status === 'HAS_ERRORS'
                                  ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                                  : item.status === 'DROPPED_OFF'
                                  ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                                  : 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                              }`}
                            >
                              {item.status}
                            </span>
                          </td>

                          {/* Last Seen IST */}
                          <td className="py-3 px-4 font-mono text-gray-400 text-[11px]">
                            {item.last_seen_ist}
                          </td>

                          {/* Action */}
                          <td className="py-3 px-4 text-right">
                            <button
                              onClick={() => openJourney(item.user_email || item.device_id)}
                              className="px-3 py-1.5 rounded-lg bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 text-cyan-300 font-medium text-xs transition-all inline-flex items-center gap-1"
                            >
                              Inspect Journey
                              <ChevronRight className="w-3.5 h-3.5" />
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
          <div className="space-y-4">
            {/* Filter Bar */}
            <div className="flex flex-wrap gap-3 items-center justify-between bg-[#0D121D] p-4 rounded-2xl border border-white/10">
              <div className="flex flex-wrap items-center gap-3">
                {/* Format Filter */}
                <select
                  value={adFormatFilter}
                  onChange={(e) => setAdFormatFilter(e.target.value)}
                  className="px-3 py-1.5 bg-[#0B0F17] border border-white/10 rounded-xl text-xs text-gray-300 focus:outline-none focus:border-cyan-500"
                >
                  <option value="ALL">All Formats</option>
                  <option value="rewarded">Rewarded Video</option>
                  <option value="interstitial">Interstitial</option>
                </select>

                {/* Status Filter */}
                <select
                  value={adStatusFilter}
                  onChange={(e) => setAdStatusFilter(e.target.value)}
                  className="px-3 py-1.5 bg-[#0B0F17] border border-white/10 rounded-xl text-xs text-gray-300 focus:outline-none focus:border-cyan-500"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="DELIVERED">Delivered / Displayed</option>
                  <option value="IMPRESSION">Impression</option>
                  <option value="REWARDED">Rewarded Earned</option>
                  <option value="FAILED">Failed / No Fill</option>
                </select>

                {/* Date Filter */}
                <input
                  type="date"
                  value={adDateFilter}
                  onChange={(e) => setAdDateFilter(e.target.value)}
                  className="px-3 py-1.5 bg-[#0B0F17] border border-white/10 rounded-xl text-xs text-gray-300 focus:outline-none focus:border-cyan-500 font-mono"
                />

                <button
                  onClick={fetchAdFlow}
                  className="px-3 py-1.5 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 hover:bg-cyan-500/20 text-xs font-medium flex items-center gap-1"
                >
                  <Filter className="w-3.5 h-3.5" />
                  Filter Ads
                </button>
              </div>

              <div className="text-xs text-gray-400 font-mono">
                Showing {adFlowEvents.length} ad events
              </div>
            </div>

            {/* Ad Flow Table */}
            <div className="bg-[#0D121D] border border-white/10 rounded-2xl overflow-hidden shadow-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-white/10 bg-white/[0.02] text-gray-400 font-mono">
                      <th className="py-3 px-4 font-medium">TIME (IST)</th>
                      <th className="py-3 px-4 font-medium">USER / DEVICE</th>
                      <th className="py-3 px-4 font-medium">TRIGGER / PLACEMENT</th>
                      <th className="py-3 px-4 font-medium">FORMAT</th>
                      <th className="py-3 px-4 font-medium">WINNER NETWORK</th>
                      <th className="py-3 px-4 font-medium">REVENUE / ECPM</th>
                      <th className="py-3 px-4 font-medium">STATUS</th>
                      <th className="py-3 px-4 font-medium">ERROR / REASON</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {loadingAdFlow ? (
                      <tr>
                        <td colSpan="8" className="text-center py-12 text-gray-500">
                          <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-cyan-400" />
                          Loading ad lifecycle logs...
                        </td>
                      </tr>
                    ) : adFlowEvents.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="text-center py-12 text-gray-500 font-mono">
                          No ad lifecycle events matching filter.
                        </td>
                      </tr>
                    ) : (
                      adFlowEvents.map((ad, idx) => (
                        <tr key={idx} className="hover:bg-white/[0.02] transition-colors">
                          {/* Time IST */}
                          <td className="py-3 px-4 font-mono text-gray-300 text-[11px] whitespace-nowrap">
                            {ad.timestamp_ist}
                          </td>

                          {/* User */}
                          <td className="py-3 px-4">
                            <div className="font-semibold text-white">{ad.user_email || 'Anonymous'}</div>
                            <div className="text-[10px] text-gray-500">{ad.country || 'IN'}</div>
                          </td>

                          {/* Placement */}
                          <td className="py-3 px-4">
                            <span className="px-2 py-0.5 rounded bg-white/5 border border-white/10 text-cyan-300 font-mono text-[11px]">
                              {ad.placement}
                            </span>
                            {ad.trigger_reason && ad.trigger_reason !== 'unspecified' && (
                              <div className="text-[10px] text-gray-500 mt-0.5">why: {ad.trigger_reason}</div>
                            )}
                          </td>

                          {/* Format */}
                          <td className="py-3 px-4">
                            <span className="capitalize font-medium text-white">{ad.ad_type}</span>
                          </td>

                          {/* Winner Network */}
                          <td className="py-3 px-4">
                            <span className="capitalize text-emerald-400 font-semibold">{ad.ad_network}</span>
                          </td>

                          {/* Revenue */}
                          <td className="py-3 px-4 font-mono">
                            <div className="text-amber-300 font-bold">${(ad.revenue || 0).toFixed(6)}</div>
                            <div className="text-[10px] text-gray-500">eCPM: ${(ad.ecpm || 0).toFixed(2)}</div>
                          </td>

                          {/* Status */}
                          <td className="py-3 px-4">
                            <span
                              className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                                ad.status === 'DELIVERED' || ad.status === 'IMPRESSION' || ad.status === 'REWARDED'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-red-500/10 text-red-400 border border-red-500/20'
                              }`}
                            >
                              {ad.status}
                            </span>
                          </td>

                          {/* Error */}
                          <td className="py-3 px-4 text-red-400 text-[11px]">
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
      </main>

      {/* USER JOURNEY MODAL / DRAWER */}
      {selectedJourney && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex justify-end">
          <div className="w-full max-w-2xl bg-[#0D121D] border-l border-white/10 h-full overflow-y-auto p-6 flex flex-col shadow-2xl">
            {/* Drawer Header */}
            <div className="flex items-center justify-between pb-4 border-b border-white/10 mb-6">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center border border-cyan-500/20">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Full User Journey Inspector</h3>
                  <p className="text-xs text-gray-400 font-mono">
                    {selectedJourney.user?.email || selectedJourney.user?.device_id}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedJourney(null)}
                className="p-2 rounded-lg bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-all"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* User Meta Card */}
            <div className="p-4 rounded-xl bg-white/[0.03] border border-white/5 mb-6 grid grid-cols-2 gap-3 text-xs">
              <div>
                <span className="text-gray-500">Device Model:</span>
                <p className="text-white font-medium">{selectedJourney.user?.device_model || 'Unknown'}</p>
              </div>
              <div>
                <span className="text-gray-500">Operating System:</span>
                <p className="text-white font-medium">{selectedJourney.user?.os_version || 'Android'}</p>
              </div>
              <div>
                <span className="text-gray-500">Source:</span>
                <p className={selectedJourney.user?.is_play_store ? 'text-emerald-400 font-bold' : 'text-amber-400'}>
                  {selectedJourney.user?.is_play_store ? '🟢 Google Play Store' : 'Direct APK'}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Country / Net:</span>
                <p className="text-white font-medium">
                  {selectedJourney.user?.country || 'IN'} ({selectedJourney.user?.network_type || 'WiFi'})
                </p>
              </div>
            </div>

            {/* Chronological Steps */}
            <div className="flex-1 space-y-4">
              <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider font-mono">
                Step-by-Step Chronological Trail (IST)
              </h4>

              <div className="relative pl-6 border-l-2 border-white/10 space-y-6">
                {(selectedJourney.journey || []).map((step, idx) => (
                  <div key={idx} className="relative group">
                    {/* Timeline bullet */}
                    <div
                      className={`absolute -left-[31px] top-1 w-4 h-4 rounded-full border-2 border-[#0D121D] flex items-center justify-center ${
                        step.is_success
                          ? 'bg-emerald-400'
                          : 'bg-red-400'
                      }`}
                    />

                    <div className="bg-white/[0.02] border border-white/5 rounded-xl p-3.5 hover:border-white/10 transition-all">
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <span className="text-xs font-bold text-white font-mono flex items-center gap-1.5">
                          {step.step_name}
                        </span>
                        <span className="text-[10px] text-cyan-300 font-mono">
                          {step.timestamp_ist}
                        </span>
                      </div>

                      {/* Elapsed time badge */}
                      {step.elapsed_sec > 0 && (
                        <div className="text-[10px] text-gray-500 font-mono mb-2">
                          ⏱️ +{step.elapsed_sec}s since previous step
                        </div>
                      )}

                      {/* Screen duration */}
                      {step.duration_seconds > 0 && (
                        <div className="text-[11px] text-amber-300 font-mono mb-2">
                          ⌛ Spent {step.duration_seconds} seconds on screen
                        </div>
                      )}

                      {/* Error message */}
                      {step.error_message && (
                        <div className="text-[11px] text-red-400 bg-red-950/30 p-2 rounded border border-red-500/20 mb-2 font-mono">
                          ⚠️ {step.error_message}
                        </div>
                      )}

                      {/* Payload details */}
                      {step.details && Object.keys(step.details).length > 0 && (
                        <div className="text-[11px] text-gray-400 bg-black/30 p-2 rounded font-mono overflow-x-auto">
                          {JSON.stringify(step.details, null, 2)}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

