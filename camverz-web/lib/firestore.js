// ============================================
// Firestore.js → API.js Migration
// All Firestore queries replaced with API calls
// This file re-exports API functions with the same names
// so existing components don't break
// ============================================

import { 
  getMe, getUser as apiGetUser, updateMe,
  getFollowers, getFollowing, followUser as apiFollow, unfollowUser as apiUnfollow,
  getFeedPosts as apiGetFeedPosts
} from './api';

export async function getUser(uid) {
  try {
    const response = await apiGetUser(uid);
    return response.ok ? response.user : null;
  } catch {
    return null;
  }
}

export async function updateUser(uid, data) {
  await updateMe(data);
}

export async function getFollowersCount(uid) {
  try {
    const response = await apiGetUser(uid);
    return response.ok ? response.followersCount : 0;
  } catch {
    return 0;
  }
}

export async function getFollowingCount(uid) {
  try {
    const response = await apiGetUser(uid);
    return response.ok ? response.followingCount : 0;
  } catch {
    return 0;
  }
}

export async function isFollowing(currentUid, targetUid) {
  try {
    const response = await apiGetUser(targetUid);
    return response.ok ? response.isFollowing : false;
  } catch {
    return false;
  }
}

export async function followUser(currentUid, targetUid) {
  await apiFollow(targetUid);
}

export async function unfollowUser(currentUid, targetUid) {
  await apiUnfollow(targetUid);
}

export async function getFeedPosts(category, userGender) {
  try {
    const response = await apiGetFeedPosts(category);
    return response.ok ? response.posts : [];
  } catch {
    return [];
  }
}
