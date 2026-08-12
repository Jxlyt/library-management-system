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

// 用户信息
export function getProfile() {
  return request.get("/users/profile");
}

export function updateProfile(data) {
  return request.put("/users/profile", data);
}

export function changePassword(data) {
  return request.put("/users/change-password", data);
}

// 统计
export function getOverview() {
  return request.get("/stats/overview");
}

export function getBorrowRanking() {
  return request.get("/stats/borrow-ranking");
}

export function getMonthlyRanking() {
  return request.get("/stats/monthly-ranking");
}

export function getCategories() {
  return request.get("/stats/categories");
}

// 图书盲盒
export function getRandomBook() {
  return request.get("/books/random");
}
