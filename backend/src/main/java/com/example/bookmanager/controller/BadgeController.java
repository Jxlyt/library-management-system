package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Badge;
import com.example.bookmanager.entity.UserBadge;
import com.example.bookmanager.service.BadgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @GetMapping
    public ApiResponse<List<Badge>> getAllBadges() {
        return badgeService.getAllBadges();
    }

    @GetMapping("/my")
    public ApiResponse<List<UserBadge>> getUserBadges(HttpServletRequest request) {
        return badgeService.getUserBadges(getCurrentUsername(request));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserBadgeStats(HttpServletRequest request) {
        return badgeService.getUserBadgeStats(getCurrentUsername(request));
    }

    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> checkBadges(HttpServletRequest request) {
        badgeService.checkAndAwardBadges(getCurrentUsername(request));
        return badgeService.getUserBadgeStats(getCurrentUsername(request));
    }
}