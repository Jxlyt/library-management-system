package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public OrderServiceImpl(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                            AddressRepository addressRepository, UserRepository userRepository,
                            BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional
    public ApiResponse<Order> createOrder(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<CartItem> cartItems = cartItemRepository.findByUserOrderByCreatedAtDesc(user);
        if (cartItems.isEmpty()) {
            return ApiResponse.error(400, "购物车为空");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!address.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权使用该地址");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        order.setStatus("PENDING_PAYMENT");
        order.setShippingName(address.getName());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddress(address.getProvince() + address.getCity() + address.getDistrict() + " " + address.getDetail());

        double totalAmount = 0;
        double discountAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            double price = book.getSalePrice() != null ? book.getSalePrice() : 0;
            double discount = 0;
            if (book.getDiscount() != null && book.getDiscount() > 0) {
                discount = price * (1 - book.getDiscount());
            }
            double actualPrice = price * (book.getDiscount() != null ? book.getDiscount() : 1.0);
            double subtotal = actualPrice * cartItem.getQuantity();

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setBookId(book.getId());
            oi.setBookTitle(book.getTitle());
            oi.setPrice(price);
            oi.setDiscount(book.getDiscount() != null ? book.getDiscount() : 0);
            oi.setQuantity(cartItem.getQuantity());
            oi.setSubtotal(subtotal);
            orderItems.add(oi);

            // 扣减可售库存（防止超卖）
            if (book.getSaleableCopies() != null && book.getSaleableCopies() >= cartItem.getQuantity()) {
                book.setSaleableCopies(book.getSaleableCopies() - cartItem.getQuantity());
                bookRepository.save(book);
            } else {
                return ApiResponse.error(400, "《" + book.getTitle() + "》库存不足，仅剩 " + (book.getSaleableCopies() != null ? book.getSaleableCopies() : 0) + " 件");
            }

            totalAmount += price * cartItem.getQuantity();
            discountAmount += discount * cartItem.getQuantity();
        }

        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(totalAmount - discountAmount);
        order.setItems(orderItems);

        // 清空购物车
        cartItemRepository.deleteByUser(user);

        Order saved = orderRepository.save(order);
        return ApiResponse.success("下单成功", saved);
    }

    @Override
    @Transactional
    public ApiResponse<Order> payOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return ApiResponse.error(400, "订单状态不正确");
        }

        // 模拟支付成功
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return ApiResponse.success("支付成功", saved);
    }

    @Override
    @Transactional
    public ApiResponse<Order> cancelOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return ApiResponse.error(400, "只能取消待支付订单");
        }

        // 恢复库存
        for (OrderItem item : order.getItems()) {
            bookRepository.findById(item.getBookId()).ifPresent(book -> {
                if (book.getSaleableCopies() != null) {
                    book.setSaleableCopies(book.getSaleableCopies() + item.getQuantity());
                    bookRepository.save(book);
                }
            });
        }

        order.setStatus("CANCELLED");
        return ApiResponse.success("订单已取消", orderRepository.save(order));
    }

    @Override
    @Transactional
    public ApiResponse<Order> confirmReceive(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            return ApiResponse.error(400, "只能确认收货已发货订单");
        }
        order.setStatus("RECEIVED");
        return ApiResponse.success("已确认收货", orderRepository.save(order));
    }

    @Override
    @Transactional
    public ApiResponse<Order> shipOrder(Long orderId, String shippingCompany, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"PAID".equals(order.getStatus())) {
            return ApiResponse.error(400, "只能发货已支付订单");
        }
        order.setStatus("SHIPPED");
        order.setShippingCompany(shippingCompany);
        order.setTrackingNumber(trackingNumber);
        order.setShipTime(LocalDateTime.now());
        return ApiResponse.success("发货成功", orderRepository.save(order));
    }

    @Override
    public ApiResponse<Page<Order>> getMyOrders(String username, String status, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, pageable);
        } else {
            orders = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }
        return ApiResponse.success(orders);
    }

    @Override
    public ApiResponse<Page<Order>> getAllOrders(String status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatusFilter(status, pageable);
        return ApiResponse.success(orders);
    }

    @Override
    @Transactional
    public ApiResponse<Order> updateOrderAddress(Long orderId, Long addressId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!address.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权使用该地址");
        }
        order.setShippingName(address.getName());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddress(address.getProvince() + address.getCity() + address.getDistrict() + " " + address.getDetail());
        return ApiResponse.success("地址已更新", orderRepository.save(order));
    }

    @Override
    public ApiResponse<Map<String, Object>> getSalesStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayOrders = orderRepository.countByCreatedAtBetween(todayStart, todayEnd);
        double todaySales = orderRepository.sumPayAmountByPaidAndCreatedAtBetween(todayStart, todayEnd);
        double monthSales = orderRepository.sumPayAmountByPaidAndCreatedAtBetween(monthStart, todayEnd);
        long totalOrders = orderRepository.count();

        List<Object[]> topSelling = orderRepository.findTopSellingBooks(Pageable.ofSize(10));

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayOrders", todayOrders);
        stats.put("todaySales", todaySales);
        stats.put("monthSales", monthSales);
        stats.put("totalOrders", totalOrders);
        stats.put("topSelling", topSelling);
        return ApiResponse.success(stats);
    }
}