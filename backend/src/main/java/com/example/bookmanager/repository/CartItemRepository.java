package com.example.bookmanager.repository;

import com.example.bookmanager.entity.CartItem;
import com.example.bookmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserOrderByCreatedAtDesc(User user);
    Optional<CartItem> findByUserAndBookId(User user, Long bookId);
    void deleteByUser(User user);
    int countByUser(User user);
}