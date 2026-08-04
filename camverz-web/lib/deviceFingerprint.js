/**
 * Web Device Fingerprinting Helper
 * Generates and persists a unique Device ID in localStorage + cookies.
 */

function simpleHash(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash |= 0;
  }
  return Math.abs(hash).toString(36);
}

function getBrowserFingerprint() {
  if (typeof window === 'undefined') return 'server';

  const userAgent = navigator.userAgent || '';
  const language = navigator.language || '';
  const screenRes = `${window.screen.width}x${window.screen.height}x${window.screen.colorDepth}`;
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || '';

  // Generate canvas fingerprint hash
  let canvasHash = '';
  try {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.textBaseline = 'top';
      ctx.font = "14px 'Arial'";
      ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = '#f60';
      ctx.fillRect(125, 1, 62, 20);
      ctx.fillStyle = '#069';
      ctx.fillText('CamverzWebDeviceFP,123', 2, 15);
      ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
      ctx.fillText('CamverzWebDeviceFP,123', 4, 17);
      canvasHash = simpleHash(canvas.toDataURL());
    }
  } catch (e) {
    canvasHash = 'no_canvas';
  }

  const rawString = `${userAgent}|${language}|${screenRes}|${timezone}|${canvasHash}`;
  return `fp_${simpleHash(rawString)}`;
}

export function getDeviceId() {
  if (typeof window === 'undefined') return null;

  const STORAGE_KEY = 'camverz_device_id';
  let deviceId = localStorage.getItem(STORAGE_KEY);

  if (!deviceId) {
    // Check cookie fallback
    const match = document.cookie.match(new RegExp('(^| )' + STORAGE_KEY + '=([^;]+)'));
    if (match) {
      deviceId = match[2];
    }
  }

  if (!deviceId) {
    // Generate new unique device ID combining timestamp, random string, and browser fingerprint
    const fp = getBrowserFingerprint();
    const random = Math.random().toString(36).substring(2, 10);
    deviceId = `dev_${fp}_${Date.now().toString(36)}_${random}`;
  }

  // Persist in localStorage and long-lived cookie (10 years)
  try {
    localStorage.setItem(STORAGE_KEY, deviceId);
    document.cookie = `${STORAGE_KEY}=${deviceId}; path=/; max-age=315360000; SameSite=Lax`;
  } catch (e) {
    console.warn('Could not persist deviceId:', e);
  }

  return deviceId;
}
