import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({ baseURL: "/api", timeout: 10000 });
request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
request.interceptors.response.use(
  (response) => { const res = response.data; if (res.code !== 200) { ElMessage.error(res.message || "请求失败"); return Promise.reject(new Error(res.message)); } return res; },
  (error) => { ElMessage.error(error.response?.data?.message || "网络错误"); return Promise.reject(error); }
);

// 盲盒
export function createBlindBox(data) { return request.post('/blind-box/create', data); }
export function openBlindBox(category) { return request.post('/blind-box/open', category ? { category } : {}); }
export function getMyBlindBoxes(params) { return request.get('/blind-box/my', { params }); }
export function getBlindBoxCount() { return request.get('/blind-box/count'); }

// 拍卖
export function createAuction(data) { return request.post('/auctions/create', data); }
export function placeBid(auctionId, bidAmount) { return request.post(`/auctions/${auctionId}/bid`, { bidAmount }); }
export function getAuctions(params) { return request.get('/auctions', { params }); }
export function getAuctionDetail(auctionId) { return request.get(`/auctions/${auctionId}`); }
export function getAuctionBids(auctionId) { return request.get(`/auctions/${auctionId}/bids`); }
export function cancelAuction(auctionId) { return request.post(`/auctions/${auctionId}/cancel`); }