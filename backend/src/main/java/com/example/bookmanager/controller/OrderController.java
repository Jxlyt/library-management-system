package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Order;
import com.example.bookmanager.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping("/create")
    public ApiResponse<Order> createOrder(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        return orderService.createOrder(getCurrentUsername(request), body.get("addressId"));
    }

    @PostMapping("/pay/{orderId}")
    public ApiResponse<Order> payOrder(@PathVariable Long orderId, HttpServletRequest request) {
        return orderService.payOrder(orderId, getCurrentUsername(request));
    }

    @PostMapping("/cancel/{orderId}")
    public ApiResponse<Order> cancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        return orderService.cancelOrder(orderId, getCurrentUsername(request));
    }

    @PostMapping("/receive/{orderId}")
    public ApiResponse<Order> confirmReceive(@PathVariable Long orderId, HttpServletRequest request) {
        return orderService.confirmReceive(orderId, getCurrentUsername(request));
    }

    @PostMapping("/ship/{orderId}")
    public ApiResponse<Order> shipOrder(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return orderService.shipOrder(orderId, body.get("shippingCompany"), body.get("trackingNumber"));
    }

    @PostMapping("/address/{orderId}")
    public ApiResponse<Order> updateOrderAddress(@PathVariable Long orderId, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        return orderService.updateOrderAddress(orderId, body.get("addressId"), getCurrentUsername(request));
    }

    @GetMapping("/my")
    public ApiResponse<Page<Order>> getMyOrders(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.getMyOrders(getCurrentUsername(request), status, pageable);
    }

    @GetMapping("/all")
    public ApiResponse<Page<Order>> getAllOrders(@RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.getAllOrders(status, pageable);
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getSalesStats() {
        return orderService.getSalesStats();
    }
}