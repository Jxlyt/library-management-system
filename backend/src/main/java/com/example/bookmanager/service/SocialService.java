package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SocialService {

    // ======== 收藏 ========
    ApiResponse<?> addFavorite(Long bookId, String username);
    ApiResponse<?> removeFavorite(Long bookId, String username);
    ApiResponse<Page<Favorite>> getMyFavorites(String username, Pageable pageable);
    ApiResponse<Boolean> isFavorited(Long bookId, String username);

    // ======== 评价 ========
    ApiResponse<BookReview> addReview(Long bookId, String username, Integer rating, String comment);
    ApiResponse<BookReview> addFullReview(Long bookId, String username, String title, String content, Integer rating);
    ApiResponse<Page<BookReview>> getBookReviews(Long bookId, Pageable pageable);
    ApiResponse<Map<String, Object>> getBookRatingStats(Long bookId);
    ApiResponse<?> likeReview(Long reviewId, String username);
    ApiResponse<?> pinReview(Long reviewId);
    ApiResponse<?> unpinReview(Long reviewId);
    ApiResponse<Page<BookReview>> getPinnedReviews(Pageable pageable);

    // ======== 读书笔记 ========
    ApiResponse<ReadingNote> createNote(Long bookId, String username, String title, String content, Boolean isPublic);
    ApiResponse<ReadingNote> updateNote(Long noteId, String username, String title, String content, Boolean isPublic);
    ApiResponse<?> deleteNote(Long noteId, String username);
    ApiResponse<Page<ReadingNote>> getMyNotes(String username, Pageable pageable);
    ApiResponse<Page<ReadingNote>> getPublicNotes(Pageable pageable);

    // ======== 阅读打卡 ========
    ApiResponse<?> checkIn(String username);
    ApiResponse<Map<String, Object>> getCheckInStats(String username);

    // ======== 关注 ========
    ApiResponse<?> followUser(Long followeeId, String username);
    ApiResponse<?> unfollowUser(Long followeeId, String username);
    ApiResponse<Page<UserFollow>> getFollowers(Long userId, Pageable pageable);
    ApiResponse<Page<UserFollow>> getFollowing(Long userId, Pageable pageable);
    ApiResponse<Boolean> isFollowing(Long userId, String username);
    ApiResponse<Map<String, Object>> getFollowStats(Long userId, String username);

    // ======== 动态 ========
    ApiResponse<Page<Activity>> getUserActivities(Long userId, Pageable pageable);
    ApiResponse<Page<Activity>> getFollowedActivities(String username, Pageable pageable);
}