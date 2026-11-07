// ==============================================================================
// CAMVERZ SUBSCRIPTION & VIP PASS TRACKING SYSTEM
// Handles 1 Day, 7 Days, and 1 Month Pass tracking, expiry calculation,
// region pricing, and ad-free status checks across Web & App.
// ==============================================================================

export const REGIONAL_COUNTRIES = ['pakistan', 'bangladesh', 'taiwan', 'thailand', 'sri lanka', 'philippines', 'indonesia', 'vietnam', 'pk', 'bd', 'tw', 'th', 'lk', 'ph', 'id', 'vn'];

// Master Pricing Matrix (Exact match for India, Regional, International across App & Web)
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
      with_ads: { price: 1.30, currency: '$', displayPrice: '$1.30', link: 'https://rzp.io/rzp/bHc6zCa' },
      no_ads: { price: 1.70, currency: '$', displayPrice: '$1.70', link: 'https://rzp.io/rzp/U4o0bEP' }
    },
    international: {
      with_ads: { price: 4.99, currency: '$', displayPrice: '$4.99', link: 'https://rzp.io/rzp/m7lJpaEG' },
      no_ads: { price: 6.99, currency: '$', displayPrice: '$6.99', link: 'https://rzp.io/rzp/RUN9vJe1' }
    }
  },
  '7-days': {
    id: '7-days',
    title: '7 Days Weekly Pass',
    durationDays: 7,
    durationHours: 168,
    india: {
      with_ads: { price: 249, currency: '₹', displayPrice: '₹249', link: 'https://rzp.io/rzp/eHos15KW' },
      no_ads: { price: 379, currency: '₹', displayPrice: '₹379', link: 'https://rzp.io/rzp/rujZOejh' }
    },
    regional: {
      with_ads: { price: 2.49, currency: '$', displayPrice: '$2.49', link: 'https://rzp.io/rzp/qMiXZMKm' },
      no_ads: { price: 3.79, currency: '$', displayPrice: '$3.79', link: 'https://rzp.io/rzp/BEBATwU' }
    },
    international: {
      with_ads: { price: 9.99, currency: '$', displayPrice: '$9.99', link: 'https://rzp.io/rzp/KmjPHDFw' },
      no_ads: { price: 11.99, currency: '$', displayPrice: '$11.99', link: 'https://rzp.io/rzp/amIBKn1E' }
    }
  },
  '1-month': {
    id: '1-month',
    title: '1 Month VIP Package',
    durationDays: 30,
    durationHours: 720,
    india: {
      with_ads: { price: 449, currency: '₹', displayPrice: '₹449', link: 'https://rzp.io/rzp/neqa5jn' },
      no_ads: { price: 499, currency: '₹', displayPrice: '₹499', link: 'https://rzp.io/rzp/866PNkC0' }
    },
    regional: {
      with_ads: { price: 4.49, currency: '$', displayPrice: '$4.49', link: 'https://rzp.io/rzp/OXuUB5jj' },
      no_ads: { price: 4.99, currency: '$', displayPrice: '$4.99', link: 'https://rzp.io/rzp/SBKqmutM' }
    },
    international: {
      with_ads: { price: 14.99, currency: '$', displayPrice: '$14.99', link: 'https://rzp.io/rzp/70W31XiP' },
      no_ads: { price: 19.99, currency: '$', displayPrice: '$19.99', link: 'https://rzp.io/rzp/ySfZ6If' }
    }
  },
  'community-7-days': {
    id: 'community-7-days',
    title: 'Community 7 Days Pass',
    durationDays: 7,
    durationHours: 168,
    india: {
      with_ads: { price: 699, currency: '₹', displayPrice: '₹699', link: 'https://rzp.io/rzp/OMkARydl' },
      no_ads: { price: 850, currency: '₹', displayPrice: '₹850', link: 'https://rzp.io/rzp/jSEIM9i' }
    },
    regional: {
      with_ads: { price: 6.99, currency: '$', displayPrice: '$6.99', link: 'https://rzp.io/rzp/gDwePSz' },
      no_ads: { price: 8.50, currency: '$', displayPrice: '$8.50', link: 'https://rzp.io/rzp/uYltJep' }
    },
    international: {
      with_ads: { price: 11.99, currency: '$', displayPrice: '$11.99', link: 'https://rzp.io/rzp/yy6Jwq6' },
      no_ads: { price: 14.99, currency: '$', displayPrice: '$14.99', link: 'https://rzp.io/rzp/OuK9DpQ' }
    }
  },
  'community-1-month': {
    id: 'community-1-month',
    title: 'Community 1 Month VIP',
    durationDays: 30,
    durationHours: 720,
    india: {
      with_ads: { price: 1199, currency: '₹', displayPrice: '₹1199', link: 'https://rzp.io/rzp/WFeYvTyy' },
      no_ads: { price: 1399, currency: '₹', displayPrice: '₹1399', link: 'https://rzp.io/rzp/dDMMX0vd' }
    },
    regional: {
      with_ads: { price: 11.99, currency: '$', displayPrice: '$11.99', link: 'https://rzp.io/rzp/lXNc5Eg' },
      no_ads: { price: 13.99, currency: '$', displayPrice: '$13.99', link: 'https://rzp.io/rzp/2b6EuHqV' }
    },
    international: {
      with_ads: { price: 19.99, currency: '$', displayPrice: '$19.99', link: 'https://rzp.io/rzp/58Utuxto' },
      no_ads: { price: 27.99, currency: '$', displayPrice: '$27.99', link: 'https://rzp.io/rzp/wpdDVW0' }
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
  if (durationKey === 'community-7-days' || durationKey === '7-days') hoursToAdd = 168;
  if (durationKey === '1-month' || durationKey === 'community-1-month') hoursToAdd = 720;

  const expiresAtDate = new Date(now.getTime() + hoursToAdd * 60 * 60 * 1000);
  const expiresAt = expiresAtDate.toISOString();

  return { startedAt, expiresAt, hoursToAdd };
}

/**
 * Determines whether user belongs to India, Regional tier, or International
 */
export function getRegion(country) {
  if (!country) return 'india';
  const c = country.toLowerCase().trim();
  if (c === 'india' || c === 'in') return 'india';
  if (REGIONAL_COUNTRIES.includes(c)) return 'regional';
  return 'international';
}

/**
 * Resolves accurate Razorpay checkout link based on Duration, Tier (with/without ads) & Region
 */
export function getPaymentUrl(durationKey, isAdFree, country) {
  const region = getRegion(country);
  const pkg = PRICING_MATRIX[durationKey];
  if (!pkg) return 'https://rzp.io/rzp/0hqC5wl'; // Safe 1 Day fallback

  const tierKey = isAdFree ? 'no_ads' : 'with_ads';
  const regionData = pkg[region] || pkg['india'];
  const tierData = regionData[tierKey];

  return tierData ? tierData.link : 'https://rzp.io/rzp/0hqC5wl';
}

/**
 * Comprehensive Subscriptions & Ad-Free status validator
 */
export function getSubscriptionStatus(user) {
  if (!user) {
    return {
      hasActivePlan: false,
      isAdFree: false,
      planName: 'Free Member',
      expiresAt: null,
      daysLeft: 0,
      hoursLeft: 0
    };
  }

  // 1. Direct explicit ad-free flag check
  const isExplicitAdFree = Boolean(user.plan_is_ad_free || user.planIsAdFree);

  // 2. Plan name normalization
  const rawPlanName = user.plan_name || user.planName || 'Free Member';
  const planLower = rawPlanName.toLowerCase();

  // 3. Expiry check
  const expiresAtStr = user.plan_expires_at || user.planExpiresAt;
  let hasActivePlan = false;
  let isAdFree = false;
  let daysLeft = 0;
  let hoursLeft = 0;

  if (expiresAtStr) {
    const expiryDate = new Date(expiresAtStr);
    const now = new Date();

    if (expiryDate > now) {
      hasActivePlan = true;
      const diffMs = expiryDate.getTime() - now.getTime();
      hoursLeft = Math.ceil(diffMs / (1000 * 60 * 60));
      daysLeft = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

      // Determine ad-free status:
      // True if explicit flag is true OR plan name does NOT indicate "with ads"
      if (isExplicitAdFree) {
        isAdFree = true;
      } else if (!planLower.includes('with ads') && !planLower.includes('with_ads') && !planLower.includes('standard')) {
        isAdFree = true;
      }
    }
  }

  return {
    hasActivePlan,
    isAdFree,
    planName: rawPlanName,
    expiresAt: expiresAtStr,
    daysLeft,
    hoursLeft
  };
}
