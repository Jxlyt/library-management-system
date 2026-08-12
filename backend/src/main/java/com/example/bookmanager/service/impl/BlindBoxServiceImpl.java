package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.BlindBoxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BlindBoxServiceImpl implements BlindBoxService {

    private final BlindBoxRepository blindBoxRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public BlindBoxServiceImpl(BlindBoxRepository blindBoxRepository, BookRepository bookRepository,
                               UserRepository userRepository, OrderRepository orderRepository) {
        this.blindBoxRepository = blindBoxRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public ApiResponse<BlindBox> createBlindBox(Long bookId, Double price, String category) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));
        if (price == null || price <= 0) {
            price = book.getSalePrice() != null ? book.getSalePrice() : 29.9;
        }
        BlindBox box = new BlindBox();
        box.setBook(book);
        box.setPrice(price);
        box.setCategory(category);
        box.setStatus("ACTIVE");
        blindBoxRepository.save(box);
        return ApiResponse.success("盲盒创建成功", box);
    }

    @Override
    @Transactional
    public ApiResponse<BlindBox> openBlindBox(String username, String category) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<BlindBox> activeBoxes = blindBoxRepository.findByStatus("ACTIVE");
        if (activeBoxes.isEmpty()) {
            return ApiResponse.error(400, "暂无盲盒可购买！");
        }

        // 按分类筛选（如果有指定）
        BlindBox selected = null;
        if (category != null && !category.isEmpty()) {
            List<BlindBox> filtered = activeBoxes.stream()
                    .filter(b -> category.equals(b.getCategory()))
                    .toList();
            if (!filtered.isEmpty()) {
                int idx = (int)(Math.random() * filtered.size());
                selected = filtered.get(idx);
            }
        }

        // 随机选取
        if (selected == null) {
            int idx = (int)(Math.random() * activeBoxes.size());
            selected = activeBoxes.get(idx);
        }

        Book book = selected.getBook();
        double price = selected.getPrice() != null ? selected.getPrice() : 29.9;

        // 标记为已售
        selected.setStatus("SOLD");
        selected.setBuyer(user);
        selected.setSoldAt(LocalDateTime.now());

        // 扣减可售库存
        if (book.getSaleableCopies() != null && book.getSaleableCopies() > 0) {
            book.setSaleableCopies(book.getSaleableCopies() - 1);
            bookRepository.save(book);
        }

        blindBoxRepository.save(selected);

        // 创建订单
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber("BB" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        order.setTotalAmount(price);
        order.setDiscountAmount(0.0);
        order.setPayAmount(price);
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());

        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setBookId(book.getId());
        oi.setBookTitle(book.getTitle());
        oi.setPrice(price);
        oi.setDiscount(0.0);
        oi.setQuantity(1);
        oi.setSubtotal(price);
        order.setItems(new ArrayList<>(List.of(oi)));

        orderRepository.save(order);

        // 将订单信息附加到返回
        Map<String, Object> extra = new HashMap<>();
        extra.put("orderId", order.getId());
        extra.put("orderNumber", order.getOrderNumber());
        return ApiResponse.success("恭喜获得：《" + selected.getBook().getTitle() + "》", selected, extra);
    }

    @Override
    public ApiResponse<Page<BlindBox>> getMyBlindBoxes(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Page<BlindBox> page = blindBoxRepository.findByBuyerOrderByCreatedAtDesc(user, pageable);
        return ApiResponse.success(page);
    }

    @Override
    public ApiResponse<?> getActiveCount() {
        long count = blindBoxRepository.countByStatus("ACTIVE");
        return ApiResponse.success(java.util.Map.of("count", count));
    }
}