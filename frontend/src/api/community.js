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

// 私信
export function sendMessage(data) {
  return request.post("/messages/send", data);
}

export function getConversation(otherUsername, params) {
  return request.get(`/messages/conversation/${otherUsername}`, { params });
}

export function getConversations(params) {
  return request.get("/messages/conversations", { params });
}

export function getUnreadCount() {
  return request.get("/messages/unread");
}

export function markAsRead(senderUsername) {
  return request.post(`/messages/read/${senderUsername}`);
}

// 积分等级
export function getMyLevel() {
  return request.get("/points/level");
}

// 读书小组
export function createGroup(data) {
  return request.post("/groups", data);
}

export function getGroups(params) {
  return request.get("/groups", { params });
}

export function getGroup(id) {
  return request.get(`/groups/${id}`);
}

export function getMyGroups(params) {
  return request.get("/groups/my", { params });
}

export function joinGroup(id) {
  return request.post(`/groups/${id}/join`);
}

export function leaveGroup(id) {
  return request.post(`/groups/${id}/leave`);
}

export function checkMembership(id) {
  return request.get(`/groups/${id}/check`);
}

export function getGroupPosts(groupId, params) {
  return request.get(`/groups/${groupId}/posts`, { params });
}

export function createGroupPost(groupId, data) {
  return request.post(`/groups/${groupId}/posts`, data);
}

export function deleteGroupPost(postId) {
  return request.delete(`/groups/posts/${postId}`);
}

// 问答广场
export function getQuestions(params) {
  return request.get("/qa/questions", { params });
}

export function getQuestion(id) {
  return request.get(`/qa/questions/${id}`);
}

export function createQuestion(data) {
  return request.post("/qa/questions", data);
}

export function deleteQuestion(id) {
  return request.delete(`/qa/questions/${id}`);
}

export function createAnswer(questionId, data) {
  return request.post(`/qa/questions/${questionId}/answers`, data);
}

export function getAnswers(questionId, params) {
  return request.get(`/qa/questions/${questionId}/answers`, { params });
}

export function acceptAnswer(answerId, data) {
  return request.post(`/qa/answers/${answerId}/accept`, data);
}

export function likeAnswer(answerId) {
  return request.post(`/qa/answers/${answerId}/like`);
}

// 书单分享
export function getPublicBookLists(params) {
  return request.get("/booklists/public", { params });
}

export function getMyBookLists(params) {
  return request.get("/booklists/my", { params });
}

export function createBookList(data) {
  return request.post("/booklists", data);
}

export function updateBookList(id, data) {
  return request.put(`/booklists/${id}`, data);
}

export function deleteBookList(id) {
  return request.delete(`/booklists/${id}`);
}

export function getBookList(id) {
  return request.get(`/booklists/${id}`);
}

export function getBookListItems(listId) {
  return request.get(`/booklists/${listId}/items`);
}

export function addBookToList(listId, data) {
  return request.post(`/booklists/${listId}/items`, data);
}

export function removeBookFromList(listId, bookId) {
  return request.delete(`/booklists/${listId}/items/${bookId}`);
}

export function favoriteBookList(listId) {
  return request.post(`/booklists/${listId}/favorite`);
}

export function unfavoriteBookList(listId) {
  return request.delete(`/booklists/${listId}/favorite`);
}

// 阅读挑战
export function getChallenges(params) {
  return request.get("/challenges", { params });
}

export function getChallenge(id) {
  return request.get(`/challenges/${id}`);
}

export function joinChallenge(id) {
  return request.post(`/challenges/${id}/join`);
}

export function challengeCheckIn(id, data) {
  return request.post(`/challenges/${id}/checkin`, data);
}

export function getMyChallenges(params) {
  return request.get("/challenges/my", { params });
}

export function getChallengeParticipants(id, params) {
  return request.get(`/challenges/${id}/participants`, { params });
}

export function createChallenge(data) {
  return request.post("/challenges", data);
}

export function updateChallenge(id, data) {
  return request.put(`/challenges/${id}`, data);
}

export function deleteChallenge(id) {
  return request.delete(`/challenges/${id}`);
}

export function getCheckIns(participantId, params) {
  return request.get(`/challenges/participants/${participantId}/checkins`, {
    params,
  });
}

// 徽章
export function getAllBadges() {
  return request.get("/badges");
}

export function getUserBadges() {
  return request.get("/badges/my");
}

export function getUserBadgeStats() {
  return request.get("/badges/stats");
}

export function checkBadges() {
  return request.post("/badges/check");
}
