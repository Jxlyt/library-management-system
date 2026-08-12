package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.service.SocialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    // ======== 收藏 ========

    @PostMapping("/favorites/{bookId}")
    public ApiResponse<?> addFavorite(@PathVariable Long bookId, HttpServletRequest request) {
        return socialService.addFavorite(bookId, getCurrentUsername(request));
    }

    @DeleteMapping("/favorites/{bookId}")
    public ApiResponse<?> removeFavorite(@PathVariable Long bookId, HttpServletRequest request) {
        return socialService.removeFavorite(bookId, getCurrentUsername(request));
    }

    @GetMapping("/favorites")
    public ApiResponse<Page<Favorite>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getMyFavorites(getCurrentUsername(request), pageable);
    }

    @GetMapping("/favorites/check/{bookId}")
    public ApiResponse<Boolean> isFavorited(@PathVariable Long bookId, HttpServletRequest request) {
        return socialService.isFavorited(bookId, getCurrentUsername(request));
    }

    // ======== 评价 ========

    @PostMapping("/reviews/{bookId}")
    public ApiResponse<BookReview> addReview(@PathVariable Long bookId,
                                              @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        String comment = (String) body.get("comment");
        return socialService.addReview(bookId, getCurrentUsername(request), rating, comment);
    }

    @GetMapping("/reviews/book/{bookId}")
    public ApiResponse<Page<BookReview>> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getBookReviews(bookId, pageable);
    }

    @GetMapping("/reviews/stats/{bookId}")
    public ApiResponse<Map<String, Object>> getBookRatingStats(@PathVariable Long bookId) {
        return socialService.getBookRatingStats(bookId);
    }

    // ======== 读书笔记 ========

    @PostMapping("/notes/{bookId}")
    public ApiResponse<ReadingNote> createNote(@PathVariable Long bookId,
                                                @RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean isPublic = body.get("isPublic") != null ? (Boolean) body.get("isPublic") : true;
        return socialService.createNote(bookId, getCurrentUsername(request), title, content, isPublic);
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<ReadingNote> updateNote(@PathVariable Long noteId,
                                                @RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean isPublic = body.get("isPublic") != null ? (Boolean) body.get("isPublic") : null;
        return socialService.updateNote(noteId, getCurrentUsername(request), title, content, isPublic);
    }

    @DeleteMapping("/notes/{noteId}")
    public ApiResponse<?> deleteNote(@PathVariable Long noteId, HttpServletRequest request) {
        return socialService.deleteNote(noteId, getCurrentUsername(request));
    }

    @GetMapping("/notes/my")
    public ApiResponse<Page<ReadingNote>> getMyNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return socialService.getMyNotes(getCurrentUsername(request), pageable);
    }

    @GetMapping("/notes/public")
    public ApiResponse<Page<ReadingNote>> getPublicNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getPublicNotes(pageable);
    }

    // ======== 评价增强 ========

    @PostMapping("/reviews/{bookId}/full")
    public ApiResponse<BookReview> addFullReview(@PathVariable Long bookId,
                                                  @RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        return socialService.addFullReview(bookId, getCurrentUsername(request), title, content, rating);
    }

    @PostMapping("/reviews/{reviewId}/like")
    public ApiResponse<?> likeReview(@PathVariable Long reviewId, HttpServletRequest request) {
        return socialService.likeReview(reviewId, getCurrentUsername(request));
    }

    @PostMapping("/reviews/{reviewId}/pin")
    public ApiResponse<?> pinReview(@PathVariable Long reviewId) {
        return socialService.pinReview(reviewId);
    }

    @DeleteMapping("/reviews/{reviewId}/pin")
    public ApiResponse<?> unpinReview(@PathVariable Long reviewId) {
        return socialService.unpinReview(reviewId);
    }

    @GetMapping("/reviews/pinned")
    public ApiResponse<Page<BookReview>> getPinnedReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getPinnedReviews(pageable);
    }

    // ======== 阅读打卡 ========

    @PostMapping("/checkin")
    public ApiResponse<?> checkIn(HttpServletRequest request) {
        return socialService.checkIn(getCurrentUsername(request));
    }

    @GetMapping("/checkin/stats")
    public ApiResponse<Map<String, Object>> getCheckInStats(HttpServletRequest request) {
        return socialService.getCheckInStats(getCurrentUsername(request));
    }

    // ======== 关注 ========

    @PostMapping("/follow/{userId}")
    public ApiResponse<?> followUser(@PathVariable Long userId, HttpServletRequest request) {
        return socialService.followUser(userId, getCurrentUsername(request));
    }

    @DeleteMapping("/follow/{userId}")
    public ApiResponse<?> unfollowUser(@PathVariable Long userId, HttpServletRequest request) {
        return socialService.unfollowUser(userId, getCurrentUsername(request));
    }

    @GetMapping("/followers/{userId}")
    public ApiResponse<Page<UserFollow>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getFollowers(userId, pageable);
    }

    @GetMapping("/following/{userId}")
    public ApiResponse<Page<UserFollow>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getFollowing(userId, pageable);
    }

    @GetMapping("/follow/check/{userId}")
    public ApiResponse<Boolean> isFollowing(@PathVariable Long userId, HttpServletRequest request) {
        return socialService.isFollowing(userId, getCurrentUsername(request));
    }

    @GetMapping("/follow/stats/{userId}")
    public ApiResponse<Map<String, Object>> getFollowStats(@PathVariable Long userId, HttpServletRequest request) {
        return socialService.getFollowStats(userId, getCurrentUsername(request));
    }

    // ======== 动态 ========

    @GetMapping("/activities/user/{userId}")
    public ApiResponse<Page<Activity>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getUserActivities(userId, pageable);
    }

    @GetMapping("/activities/feed")
    public ApiResponse<Page<Activity>> getFollowedActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return socialService.getFollowedActivities(getCurrentUsername(request), pageable);
    }
}