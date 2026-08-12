import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message));
    }
    return res;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("username");
      localStorage.removeItem("role");
      ElMessage.error("登录已过期，请重新登录");
      window.location.href = "/login";
      return Promise.reject(error);
    }
    ElMessage.error(error.message || "网络错误");
    return Promise.reject(error);
  },
);

// 收藏
export function addFavorite(bookId) {
  return request.post(`/social/favorites/${bookId}`);
}

export function removeFavorite(bookId) {
  return request.delete(`/social/favorites/${bookId}`);
}

export function getMyFavorites(params) {
  return request.get("/social/favorites", { params });
}

export function isFavorited(bookId) {
  return request.get(`/social/favorites/check/${bookId}`);
}

// 评价
export function addReview(bookId, data) {
  return request.post(`/social/reviews/${bookId}`, data);
}

export function getBookReviews(bookId, params) {
  return request.get(`/social/reviews/book/${bookId}`, { params });
}

export function getBookRatingStats(bookId) {
  return request.get(`/social/reviews/stats/${bookId}`);
}

// 读书笔记
export function createNote(bookId, data) {
  return request.post(`/social/notes/${bookId}`, data);
}

export function updateNote(noteId, data) {
  return request.put(`/social/notes/${noteId}`, data);
}

export function deleteNote(noteId) {
  return request.delete(`/social/notes/${noteId}`);
}

export function getMyNotes(params) {
  return request.get("/social/notes/my", { params });
}

export function getPublicNotes(params) {
  return request.get("/social/notes/public", { params });
}

// 阅读打卡
export function checkIn() {
  return request.post("/social/checkin");
}

export function getCheckInStats() {
  return request.get("/social/checkin/stats");
}

// 完整书评
export function addFullReview(bookId, data) {
  return request.post(`/social/reviews/${bookId}/full`, data);
}

export function likeReview(reviewId) {
  return request.post(`/social/reviews/${reviewId}/like`);
}

export function pinReview(reviewId) {
  return request.post(`/social/reviews/${reviewId}/pin`);
}

export function unpinReview(reviewId) {
  return request.delete(`/social/reviews/${reviewId}/pin`);
}

export function getPinnedReviews(params) {
  return request.get("/social/reviews/pinned", { params });
}

// 关注
export function followUser(userId) {
  return request.post(`/social/follow/${userId}`);
}

export function unfollowUser(userId) {
  return request.delete(`/social/follow/${userId}`);
}

export function getFollowers(userId, params) {
  return request.get(`/social/followers/${userId}`, { params });
}

export function getFollowing(userId, params) {
  return request.get(`/social/following/${userId}`, { params });
}

export function isFollowing(userId) {
  return request.get(`/social/follow/check/${userId}`);
}

export function getFollowStats(userId) {
  return request.get(`/social/follow/stats/${userId}`);
}

// 动态
export function getUserActivities(userId, params) {
  return request.get(`/social/activities/user/${userId}`, { params });
}

export function getFollowedActivities(params) {
  return request.get("/social/activities/feed", { params });
}