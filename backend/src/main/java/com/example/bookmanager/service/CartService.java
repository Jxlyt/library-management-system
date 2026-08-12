package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.CartItem;

import java.util.List;

public interface CartService {
    ApiResponse<List<CartItem>> getCart(String username);
    ApiResponse<CartItem> addToCart(Long bookId, String username, Integer quantity);
    ApiResponse<CartItem> updateCartItem(Long cartItemId, String username, Integer quantity);
    ApiResponse<Void> removeFromCart(Long cartItemId, String username);
    ApiResponse<Void> clearCart(String username);
    ApiResponse<Integer> getCartCount(String username);
}