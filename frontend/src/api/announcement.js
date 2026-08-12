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

export function getAnnouncements(params) {
  return request.get("/announcements", { params });
}

export function getLatestAnnouncements() {
  return request.get("/announcements/latest");
}

export function createAnnouncement(data) {
  return request.post("/announcements", data);
}

export function updateAnnouncement(id, data) {
  return request.put(`/announcements/${id}`, data);
}

export function deleteAnnouncement(id) {
  return request.delete(`/announcements/${id}`);
}