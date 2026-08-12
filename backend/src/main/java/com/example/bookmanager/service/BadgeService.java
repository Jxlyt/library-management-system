package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Badge;
import com.example.bookmanager.entity.UserBadge;

import java.util.List;
import java.util.Map;

public interface BadgeService {
    ApiResponse<List<Badge>> getAllBadges();
    ApiResponse<List<UserBadge>> getUserBadges(String username);
    ApiResponse<Map<String, Object>> getUserBadgeStats(String username);
    ApiResponse<UserBadge> awardBadge(String username, String badgeCode);
    void checkAndAwardBadges(String username);
}