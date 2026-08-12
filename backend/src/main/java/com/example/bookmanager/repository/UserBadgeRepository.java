package com.example.bookmanager.repository;

import com.example.bookmanager.entity.UserBadge;
import com.example.bookmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserOrderByAwardedAtDesc(User user);
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);
    long countByUserId(Long userId);
}