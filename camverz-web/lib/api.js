import axios from 'axios';

// ============================================
// API CLIENT — Replaces Firebase SDK
// Change API_URL for production
// ============================================
const API_URL = process.env.NEXT_PUBLIC_API_URL || 'https://android-9t8m.onrender.com/api';

const api = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// JWT Token management
const TOKEN_KEY = 'camverz_token';

export function getToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_KEY);
}

// Auto-attach JWT to every request
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auto-handle 401 (expired token)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      removeToken();
      // Optionally redirect to login
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new Event('auth:expired'));
      }
    }
    return Promise.reject(error);
  }
);

// ============================================
// AUTH API
// ============================================
import { getReferral } from './affiliateTracker';

export async function authWithGoogle(idToken) {
  const affiliateRef = getReferral();
  const { data } = await api.post('/auth/google', { idToken, affiliateRef });
  if (data.token) setToken(data.token);
  return data;
}

export async function refreshToken() {
  const { data } = await api.post('/auth/refresh');
  if (data.token) setToken(data.token);
  return data;
}

// ============================================
// USER API
// ============================================
export async function getMe() {
  const { data } = await api.get('/users/me');
  return data;
}

export async function getUser(userId) {
  const { data } = await api.get(`/users/${userId}`);
  return data;
}

export async function updateMe(updates) {
  const { data } = await api.put('/users/me', updates);
  return data;
}

export async function deleteAccount() {
  const { data } = await api.delete('/users/me');
  removeToken();
  return data;
}

export async function followUser(userId) {
  const { data } = await api.post(`/users/${userId}/follow`);
  return data;
}

export async function unfollowUser(userId) {
  const { data } = await api.delete(`/users/${userId}/follow`);
  return data;
}

export async function getFollowers(userId) {
  const { data } = await api.get(`/users/${userId}/followers`);
  return data;
}

export async function getFollowing(userId) {
  const { data } = await api.get(`/users/${userId}/following`);
  return data;
}

export async function blockUser(userId) {
  const { data } = await api.post(`/users/${userId}/block`);
  return data;
}

export async function unblockUser(userId) {
  const { data } = await api.delete(`/users/${userId}/block`);
  return data;
}

export async function getBlockedUsers() {
  const { data } = await api.get('/users/me/blocked');
  return data;
}

export async function reportUser(userId, reason) {
  const { data } = await api.post(`/users/${userId}/report`, { reason });
  return data;
}

// ============================================
// POST API
// ============================================
export async function getFeedPosts(category = 'all', limit = 50, offset = 0) {
  const { data } = await api.get('/posts', { params: { category, limit, offset } });
  return data;
}

export async function getUserPosts(userId, limit = 50, offset = 0) {
  const { data } = await api.get('/posts', { params: { userId, limit, offset } });
  return data;
}

export async function getPost(postId) {
  const { data } = await api.get(`/posts/${postId}`);
  return data;
}

export async function createPost(text, category) {
  const { data } = await api.post('/posts', { text, category });
  return data;
}

export async function deletePost(postId) {
  const { data } = await api.delete(`/posts/${postId}`);
  return data;
}

export async function toggleLike(postId) {
  const { data } = await api.post(`/posts/${postId}/like`);
  return data;
}

export async function getComments(postId) {
  const { data } = await api.get(`/posts/${postId}/comments`);
  return data;
}

export async function addComment(postId, text) {
  const { data } = await api.post(`/posts/${postId}/comments`, { text });
  return data;
}

export async function deleteComment(postId, commentId) {
  const { data } = await api.delete(`/posts/${postId}/comments/${commentId}`);
  return data;
}

// ============================================
// CHAT API
// ============================================
export async function getChats() {
  const { data } = await api.get('/chats');
  return data;
}

export async function getMessages(chatId, limit = 50, before = null) {
  const { data } = await api.get(`/chats/${chatId}/messages`, { params: { limit, before } });
  return data;
}

export async function sendMessage(targetUserId, text) {
  const { data } = await api.post(`/chats/${targetUserId}/messages`, { text });
  return data;
}

export async function markChatRead(chatId) {
  const { data } = await api.put(`/chats/${chatId}/read`);
  return data;
}

export async function deleteChat(chatId) {
  const { data } = await api.delete(`/chats/${chatId}`);
  return data;
}

export async function getUnreadCount() {
  const { data } = await api.get('/chats/unread-count');
  return data;
}

// ============================================
// NOTIFICATION API
// ============================================
export async function getNotifications(limit = 50, offset = 0) {
  const { data } = await api.get('/notifications', { params: { limit, offset } });
  return data;
}

export async function markNotificationRead(notificationId) {
  const { data } = await api.put(`/notifications/${notificationId}/read`);
  return data;
}

export async function markAllNotificationsRead() {
  const { data } = await api.put('/notifications/read-all');
  return data;
}

export async function getNotificationUnreadCount() {
  const { data } = await api.get('/notifications/unread-count');
  return data;
}

// ============================================
// FRIEND API
// ============================================
export async function sendFriendRequest(targetUserId) {
  const { data } = await api.post('/friends/request', { targetUserId });
  return data;
}

export async function acceptFriendRequest(requestId) {
  const { data } = await api.put(`/friends/request/${requestId}/accept`);
  return data;
}

export async function rejectFriendRequest(requestId) {
  const { data } = await api.put(`/friends/request/${requestId}/reject`);
  return data;
}

export async function getFriendRequests(type = 'received') {
  const { data } = await api.get('/friends/requests', { params: { type } });
  return data;
}

export async function getFriendStatus(userId) {
  const { data } = await api.get(`/friends/status/${userId}`);
  return data;
}

// ============================================
// VERIFICATION API
// ============================================
export async function verifyGender(imageFile) {
  const formData = new FormData();
  formData.append('image', imageFile);
  const { data } = await api.post('/verify/gender', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

// ============================================
// WEBRTC API
// ============================================
export async function getIceServers(useTurn = false) {
  const { data } = await api.get('/webrtc/ice', { params: { useTurn } });
  return data;
}

// ============================================
// AFFILIATE API
// ============================================
export async function getAffiliateMe() {
  const { data } = await api.get('/affiliate/me');
  return data;
}

export async function applyAffiliate(details) {
  const { data } = await api.post('/affiliate/apply', details);
  return data;
}

export async function verifyAffiliateBio() {
  const { data } = await api.post('/affiliate/linkedin/verify-bio');
  return data;
}

export async function verifyInstagramBio() {
  const { data } = await api.post('/affiliate/verify/instagram-bio');
  return data;
}

export async function verifyYoutubeBio() {
  const { data } = await api.post('/affiliate/verify/youtube-bio');
  return data;
}

export async function verifyOtherBio() {
  const { data } = await api.post('/affiliate/verify/other-bio');
  return data;
}

export async function resetAffiliateVerification() {
  const { data } = await api.post('/affiliate/reset-verification');
  return data;
}

export async function updateAffiliateLinks(links) {
  const { data } = await api.post('/affiliate/update-links', links);
  return data;
}

export async function adminListAffiliates() {
  const { data } = await api.get('/affiliate/admin/list');
  return data;
}

export async function adminApproveAffiliate(id) {
  const { data } = await api.put(`/affiliate/admin/${id}/approve`);
  return data;
}

export async function adminUpdateAffiliate(id, updates) {
  const { data } = await api.put(`/affiliate/admin/${id}/update`, updates);
  return data;
}

export default api;
