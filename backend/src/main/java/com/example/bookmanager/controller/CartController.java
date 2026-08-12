package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.CartItem;
import com.example.bookmanager.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @GetMapping
    public ApiResponse<List<CartItem>> getCart(HttpServletRequest request) {
        return cartService.getCart(getCurrentUsername(request));
    }

    @GetMapping("/count")
    public ApiResponse<Integer> getCartCount(HttpServletRequest request) {
        return cartService.getCartCount(getCurrentUsername(request));
    }

    @PostMapping("/add/{bookId}")
    public ApiResponse<CartItem> addToCart(@PathVariable Long bookId,
                                           @RequestBody(required = false) Map<String, Integer> body,
                                           HttpServletRequest request) {
        Integer quantity = body != null ? body.getOrDefault("quantity", 1) : 1;
        return cartService.addToCart(bookId, getCurrentUsername(request), quantity);
    }

    @PutMapping("/{cartItemId}")
    public ApiResponse<CartItem> updateCartItem(@PathVariable Long cartItemId,
                                                @RequestBody Map<String, Integer> body,
                                                HttpServletRequest request) {
        return cartService.updateCartItem(cartItemId, getCurrentUsername(request), body.get("quantity"));
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<Void> removeFromCart(@PathVariable Long cartItemId, HttpServletRequest request) {
        return cartService.removeFromCart(cartItemId, getCurrentUsername(request));
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clearCart(HttpServletRequest request) {
        return cartService.clearCart(getCurrentUsername(request));
    }
}