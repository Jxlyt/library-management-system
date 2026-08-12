import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

// 请求拦截器：添加 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：统一处理错误
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

// 图书相关 API
export function getBooks(params) {
  return request.get("/books", { params });
}

export function getBookById(id) {
  return request.get(`/books/${id}`);
}

export function createBook(data) {
  return request.post("/books", data);
}

export function updateBook(id, data) {
  return request.put(`/books/${id}`, data);
}

export function deleteBook(id) {
  return request.delete(`/books/${id}`);
}

// 书籍占卜
export function getFortuneBook() {
  return request.get("/books/fortune");
}

// 弹幕书摘
export function getLatestQuotes(limit = 10) {
  return request.get("/books/quotes", { params: { limit } });
}

export function addBookQuote(data) {
  return request.post("/books/quotes", data);
}

// 按封面颜色推荐
export function recommendByColor(color) {
  return request.get(`/books/color/${color}`);
}

// 封面AI解读
export function getCoverInterpretation(bookId) {
  return request.get(`/books/${bookId}/interpretation`);
}
