package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface OrderService {
    ApiResponse<Order> createOrder(String username, Long addressId);
    ApiResponse<Order> payOrder(Long orderId, String username);
    ApiResponse<Order> cancelOrder(Long orderId, String username);
    ApiResponse<Order> confirmReceive(Long orderId, String username);
    ApiResponse<Order> shipOrder(Long orderId, String shippingCompany, String trackingNumber);
    ApiResponse<Page<Order>> getMyOrders(String username, String status, Pageable pageable);
    ApiResponse<Page<Order>> getAllOrders(String status, Pageable pageable);
    ApiResponse<Order> updateOrderAddress(Long orderId, Long addressId, String username);
    ApiResponse<Map<String, Object>> getSalesStats();
}