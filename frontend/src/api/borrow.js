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

// 借阅图书（支持自定义日期）
export function borrowBook(bookId, data = {}) {
  return request.post(`/borrows/borrow/${bookId}`, data);
}

// 归还图书
export function returnBook(borrowId) {
  return request.post(`/borrows/return/${borrowId}`);
}

// 支付罚款
export function payFine(borrowId) {
  return request.post(`/borrows/pay-fine/${borrowId}`);
}

// 续借图书
export function renewBook(borrowId) {
  return request.post(`/borrows/renew/${borrowId}`);
}

// 预约图书
export function reserveBook(bookId) {
  return request.post(`/borrows/reserve/${bookId}`);
}

// 取消预约
export function cancelReservation(reservationId) {
  return request.post(`/borrows/cancel-reservation/${reservationId}`);
}

// 我的借阅历史
export function getMyBorrowHistory(params) {
  return request.get("/borrows/my-history", { params });
}

// 我的预约
export function getMyReservations(params) {
  return request.get("/borrows/my-reservations", { params });
}

// 管理员：所有借阅记录
export function getAllBorrows(params) {
  return request.get("/borrows/all", { params });
}

// 管理员：所有预约
export function getAllReservations(params) {
  return request.get("/borrows/all-reservations", { params });
}

// 获取某本书最早归还日期
export function getEarliestReturnDate(bookId) {
  return request.get(`/borrows/earliest-return/${bookId}`);
}

// 阅读足迹时间轴
export function getTimeline() {
  return request.get("/borrows/timeline");
}

// 借阅后悔药：5分钟内撤销
export function cancelBorrow(borrowId) {
  return request.post(`/borrows/cancel/${borrowId}`);
}

// 天气联动荐书
export function weatherRecommend(weather) {
  return request.get("/borrows/weather-recommend", { params: { weather } });
}

// 时空胶囊：留言
export function leaveCapsule(bookId, content) {
  return request.post(`/borrows/capsule/${bookId}`, { content });
}

// 时空胶囊：获取随机留言
export function getRandomCapsule(bookId) {
  return request.get(`/borrows/capsule/${bookId}`);
}

// 今日书架天气（借阅总量）
export function getTodayStats() {
  return request.get("/borrows/today-stats");
}

// 今日阅读搭档
export function getTodayPartner() {
  return request.get("/borrows/partner");
}
