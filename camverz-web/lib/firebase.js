// ============================================
// Firebase.js — DEPRECATED
// This file is kept for backward compatibility only.
// All Firebase functionality has been replaced with:
// - lib/api.js (API client)
// - lib/auth-context.js (auth management)
// ============================================

// Re-export commonly used functions from api.js
export { getToken, setToken, removeToken } from './api';

// Placeholder exports for any components that still import from here
export const auth = null;
export const db = null;
export const storage = null;
export const googleProvider = null;
export const app = null;

console.warn('[DEPRECATED] firebase.js is deprecated. Use lib/api.js instead.');
