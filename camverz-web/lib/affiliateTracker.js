/**
 * Affiliate Referral Tracker — Last Click + 30-Day Cookie Attribution
 * 
 * Stores referral codes in localStorage with a 30-day TTL.
 * Last click always wins: if a new ref code is encountered, it overwrites the old one.
 * Fully compatible with Next.js SSR.
 */

const STORAGE_KEY = 'camverz_affiliate_ref';
const EXPIRY_KEY = 'camverz_affiliate_ref_expiry';
const COOKIE_DURATION_DAYS = 30;

/**
 * Capture a referral code and store it with a 30-day expiry.
 * Overwrites any existing referral (Last Click attribution).
 */
export function captureReferral(code) {
  if (typeof window === 'undefined') return;
  if (!code || typeof code !== 'string') return;

  const sanitized = code.trim().toUpperCase();
  if (sanitized.length < 2) return;

  const expiry = Date.now() + (COOKIE_DURATION_DAYS * 24 * 60 * 60 * 1000);

  localStorage.setItem(STORAGE_KEY, sanitized);
  localStorage.setItem(EXPIRY_KEY, String(expiry));

  console.log(`[Affiliate] Referral captured: ${sanitized} (expires in ${COOKIE_DURATION_DAYS} days)`);
}

/**
 * Get the currently active referral code.
 * Returns null if no referral exists or if it has expired.
 */
export function getReferral() {
  if (typeof window === 'undefined') return null;

  const code = localStorage.getItem(STORAGE_KEY);
  const expiry = localStorage.getItem(EXPIRY_KEY);

  if (!code || !expiry) return null;

  // Check if expired
  if (Date.now() > parseInt(expiry, 10)) {
    clearReferral();
    return null;
  }

  return code;
}

/**
 * Clear the stored referral code and expiry.
 */
export function clearReferral() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(STORAGE_KEY);
  localStorage.removeItem(EXPIRY_KEY);
}

/**
 * Check if there is a valid, non-expired referral.
 */
export function isReferralValid() {
  return getReferral() !== null;
}

/**
 * Check URL parameters for a ?ref= code and capture it.
 * Should be called on every page load.
 * Returns the captured code or null.
 */
export function captureFromURL() {
  if (typeof window === 'undefined') return null;

  const params = new URLSearchParams(window.location.search);
  const ref = params.get('ref');

  if (ref) {
    captureReferral(ref);

    // Clean the URL to remove the ref param (cosmetic clean)
    const url = new URL(window.location.href);
    url.searchParams.delete('ref');
    window.history.replaceState({}, '', url.toString());

    return ref.trim().toUpperCase();
  }

  return null;
}
