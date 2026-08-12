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
    ElMessage.error(error.response?.data?.message || "网络错误");
    return Promise.reject(error);
  }
);

// 购物车
export function getCart() { return request.get('/cart'); }
export function getCartCount() { return request.get('/cart/count'); }
export function addToCart(bookId, quantity = 1) { return request.post(`/cart/add/${bookId}`, { quantity }); }
export function updateCartItem(cartItemId, quantity) { return request.put(`/cart/${cartItemId}`, { quantity }); }
export function removeFromCart(cartItemId) { return request.delete(`/cart/${cartItemId}`); }
export function clearCart() { return request.delete('/cart/clear'); }

// 订单
export function createOrder(addressId) { return request.post('/orders/create', { addressId }); }
export function payOrder(orderId) { return request.post(`/orders/pay/${orderId}`); }
export function cancelOrder(orderId) { return request.post(`/orders/cancel/${orderId}`); }
export function confirmReceive(orderId) { return request.post(`/orders/receive/${orderId}`); }
export function shipOrder(orderId, data) { return request.post(`/orders/ship/${orderId}`, data); }
export function updateOrderAddress(orderId, addressId) { return request.post(`/orders/address/${orderId}`, { addressId }); }
export function getMyOrders(params) { return request.get('/orders/my', { params }); }
export function getAllOrders(params) { return request.get('/orders/all', { params }); }
export function getSalesStats() { return request.get('/orders/stats'); }

// 收货地址
export function getMyAddresses() { return request.get('/addresses'); }
export function addAddress(data) { return request.post('/addresses', data); }
export function updateAddress(addressId, data) { return request.put(`/addresses/${addressId}`, data); }
export function deleteAddress(addressId) { return request.delete(`/addresses/${addressId}`); }
export function setDefaultAddress(addressId) { return request.put(`/addresses/${addressId}/default`); }