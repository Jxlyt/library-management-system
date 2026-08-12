package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Book;
import com.example.bookmanager.entity.CartItem;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.BookRepository;
import com.example.bookmanager.repository.CartItemRepository;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public ApiResponse<List<CartItem>> getCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        List<CartItem> items = cartItemRepository.findByUserOrderByCreatedAtDesc(user);
        return ApiResponse.success(items);
    }

    @Override
    @Transactional
    public ApiResponse<CartItem> addToCart(Long bookId, String username, Integer quantity) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        if (book.getSalePrice() == null || book.getSalePrice() <= 0) {
            return ApiResponse.error(400, "该图书暂不出售");
        }
        if (book.getSaleableCopies() != null && quantity > book.getSaleableCopies()) {
            return ApiResponse.error(400, "库存不足，仅剩" + book.getSaleableCopies() + "件");
        }

        var existing = cartItemRepository.findByUserAndBookId(user, bookId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + (quantity != null ? quantity : 1));
            return ApiResponse.success("已更新购物车数量", cartItemRepository.save(item));
        }

        CartItem item = new CartItem();
        item.setUser(user);
        item.setBook(book);
        item.setQuantity(quantity != null ? quantity : 1);
        return ApiResponse.success("已加入购物车", cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public ApiResponse<CartItem> updateCartItem(Long cartItemId, String username, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));
        if (!item.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return ApiResponse.success("已移除", null);
        }
        item.setQuantity(quantity);
        return ApiResponse.success("已更新", cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeFromCart(Long cartItemId, String username) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));
        if (!item.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        cartItemRepository.delete(item);
        return ApiResponse.success("已移除", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> clearCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        cartItemRepository.deleteByUser(user);
        return ApiResponse.success("购物车已清空", null);
    }

    @Override
    public ApiResponse<Integer> getCartCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(cartItemRepository.countByUser(user));
    }
}