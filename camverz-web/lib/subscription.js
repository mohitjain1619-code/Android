// ==============================================================================
// CAMVERZ SUBSCRIPTION & VIP PASS TRACKING SYSTEM
// Handles 1 Day, 10 Days, and 1 Month Pass tracking, expiry calculation,
// region pricing, and ad-free status checks across Web & App.
// ==============================================================================

export const REGIONAL_COUNTRIES = ['pakistan', 'bangladesh', 'thailand', 'pk', 'bd', 'th', 'np', 'lk', 'ph', 'id', 'vn'];

// Master Pricing Matrix (Exact match for India, Regional, International)
export const PRICING_MATRIX = {
  '1-day': {
    id: '1-day',
    title: '1 Day Pass',
    durationDays: 1,
    durationHours: 24,
    india: {
      with_ads: { price: 130, currency: '₹', displayPrice: '₹130', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 170, currency: '₹', displayPrice: '₹170', link: 'https://rzp.io/rzp/XJPN9Fk' }
    },
    regional: {
      with_ads: { price: 3, currency: '$', displayPrice: '$3', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 5, currency: '$', displayPrice: '$5', link: 'https://rzp.io/rzp/XJPN9Fk' }
    },
    international: {
      with_ads: { price: 7, currency: '$', displayPrice: '$7', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 10, currency: '$', displayPrice: '$10', link: 'https://rzp.io/rzp/XJPN9Fk' }
    }
  },
  '10-days': {
    id: '10-days',
    title: '10 Days Package',
    durationDays: 10,
    durationHours: 240,
    india: {
      with_ads: { price: 299, currency: '₹', displayPrice: '₹299', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 379, currency: '₹', displayPrice: '₹379', link: 'https://rzp.io/rzp/XJPN9Fk' }
    },
    regional: {
      with_ads: { price: 7, currency: '$', displayPrice: '$7', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 10, currency: '$', displayPrice: '$10', link: 'https://rzp.io/rzp/XJPN9Fk' }
    },
    international: {
      with_ads: { price: 15, currency: '$', displayPrice: '$15', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 19, currency: '$', displayPrice: '$19', link: 'https://rzp.io/rzp/XJPN9Fk' }
    }
  },
  '1-month': {
    id: '1-month',
    title: '1 Month VIP Package',
    durationDays: 30,
    durationHours: 720,
    india: {
      with_ads: { price: 449, currency: '₹', displayPrice: '₹449', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 499, currency: '₹', displayPrice: '₹499', link: 'https://rzp.io/rzp/XJPN9Fk' }
    },
    regional: {
      with_ads: { price: 12, currency: '$', displayPrice: '$12', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 18, currency: '$', displayPrice: '$18', link: 'https://rzp.io/rzp/XJPN9Fk' }
    },
    international: {
      with_ads: { price: 25, currency: '$', displayPrice: '$25', link: 'https://rzp.io/rzp/0hqC5wl' },
      no_ads: { price: 35, currency: '$', displayPrice: '$35', link: 'https://rzp.io/rzp/XJPN9Fk' }
    }
  }
};

/**
 * Calculates start time and exact expiration date for a pass purchase
 */
export function calculatePlanExpiry(durationKey) {
  const now = new Date();
  const startedAt = now.toISOString();
  
  let hoursToAdd = 24;
  if (durationKey === '10-days') hoursToAdd = 240;
  if (durationKey === '1-month') hoursToAdd = 720;
  
  const expiresAtDate = new Date(now.getTime() + hoursToAdd * 60 * 60 * 1000);
  const expiresAt = expiresAtDate.toISOString();
  
  return { startedAt, expiresAt, hoursAdded: hoursToAdd };
}

/**
 * Evaluates a user's subscription status, active state, time remaining, and ad status.
 */
export function getSubscriptionStatus(userData) {
  if (!userData) {
    return {
      hasActivePlan: false,
      isAdFree: false,
      tierType: 'free',
      planName: 'Free Account',
      remainingText: 'No Active Pass',
      badgeText: 'Free User',
      badgeColor: '#94A3B8',
      daysLeft: 0,
      hoursLeft: 0,
      minutesLeft: 0,
      percentRemaining: 0,
    };
  }

  const sub = userData.subscription || {};
  const expiresAtStr = sub.expiresAt || userData.planExpiresAt;
  const startedAtStr = sub.startedAt || userData.planStartedAt;

  if (!expiresAtStr) {
    return {
      hasActivePlan: false,
      isAdFree: false,
      tierType: 'free',
      planName: 'Free Member',
      remainingText: 'No Active Pass',
      badgeText: 'Free User',
      badgeColor: '#94A3B8',
      daysLeft: 0,
      hoursLeft: 0,
      minutesLeft: 0,
      percentRemaining: 0,
    };
  }

  const nowMs = Date.now();
  const expiresMs = new Date(expiresAtStr).getTime();
  const startedMs = startedAtStr ? new Date(startedAtStr).getTime() : nowMs - (24 * 60 * 60 * 1000);

  if (isNaN(expiresMs) || nowMs >= expiresMs) {
    return {
      hasActivePlan: false,
      isAdFree: false,
      tierType: 'expired',
      planName: sub.planName || 'Expired VIP Pass',
      remainingText: 'Pass Expired',
      badgeText: 'Pass Expired',
      badgeColor: '#EF4444',
      daysLeft: 0,
      hoursLeft: 0,
      minutesLeft: 0,
      percentRemaining: 0,
    };
  }

  // Calculate detailed remaining time
  const totalDiffMs = expiresMs - nowMs;
  const totalDurationMs = Math.max(1, expiresMs - startedMs);
  const percentRemaining = Math.min(100, Math.max(0, Math.round((totalDiffMs / totalDurationMs) * 100)));

  const totalMinutes = Math.floor(totalDiffMs / (1000 * 60));
  const totalHours = Math.floor(totalMinutes / 60);
  const daysLeft = Math.floor(totalHours / 24);
  const hoursLeft = totalHours % 24;
  const minutesLeft = totalMinutes % 60;

  let remainingText = '';
  if (daysLeft > 0) {
    remainingText = `${daysLeft}d ${hoursLeft}h remaining`;
  } else if (hoursLeft > 0) {
    remainingText = `${hoursLeft}h ${minutesLeft}m remaining`;
  } else {
    remainingText = `${minutesLeft}m remaining`;
  }

  const isAdFree = sub.isAdFree === true || sub.tierType === 'no_ads' || (sub.planId && sub.planId.includes('noads'));
  const planName = sub.planName || (isAdFree ? 'VIP Ad-Free Pass' : 'VIP Pass (With Ads)');

  return {
    hasActivePlan: true,
    isAdFree,
    tierType: isAdFree ? 'no_ads' : 'with_ads',
    planName,
    remainingText,
    badgeText: isAdFree ? '🚫 100% Ad-Free VIP' : '📺 VIP Standard (Ads)',
    badgeColor: isAdFree ? '#00E5FF' : '#BD00FF',
    daysLeft,
    hoursLeft,
    minutesLeft,
    percentRemaining,
    startedAt: startedAtStr,
    expiresAt: expiresAtStr,
  };
}

/**
 * Returns exact payment URL for a given package, region, and user details
 */
export function getPaymentUrl(pkgId, isAdFree, regionMode, userEmail = '', userName = '') {
  const pkg = PRICING_MATRIX[pkgId];
  if (!pkg) return null;
  
  const regionConfig = pkg[regionMode] || pkg.international;
  const tierKey = isAdFree ? 'no_ads' : 'with_ads';
  const planDetails = regionConfig[tierKey];

  if (!planDetails || !planDetails.link) return null;

  const url = new URL(planDetails.link);
  if (userEmail) url.searchParams.set('email', userEmail);
  if (userName) url.searchParams.set('name', userName);
  url.searchParams.set('pkgId', pkgId);
  url.searchParams.set('adFree', isAdFree ? 'true' : 'false');
  url.searchParams.set('region', regionMode);

  return url.toString();
}
