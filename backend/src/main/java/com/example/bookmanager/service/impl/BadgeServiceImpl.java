package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.BadgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final ReadingCheckInRepository readingCheckInRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookReviewRepository bookReviewRepository;

    public BadgeServiceImpl(BadgeRepository badgeRepository,
                            UserBadgeRepository userBadgeRepository,
                            UserRepository userRepository,
                            ReadingCheckInRepository readingCheckInRepository,
                            BorrowRecordRepository borrowRecordRepository,
                            BookReviewRepository bookReviewRepository) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userRepository = userRepository;
        this.readingCheckInRepository = readingCheckInRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookReviewRepository = bookReviewRepository;
    }

    @Override
    public ApiResponse<List<Badge>> getAllBadges() {
        return ApiResponse.success(badgeRepository.findAll());
    }

    @Override
    public ApiResponse<List<UserBadge>> getUserBadges(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(userBadgeRepository.findByUserOrderByAwardedAtDesc(user));
    }

    @Override
    public ApiResponse<Map<String, Object>> getUserBadgeStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> stats = new HashMap<>();
        List<UserBadge> userBadges = userBadgeRepository.findByUserOrderByAwardedAtDesc(user);
        stats.put("earnedCount", userBadges.size());
        stats.put("earnedBadgeIds", userBadges.stream().map(ub -> ub.getBadge().getId()).collect(Collectors.toList()));
        return ApiResponse.success(stats);
    }

    @Override
    @Transactional
    public ApiResponse<UserBadge> awardBadge(String username, String badgeCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Badge badge = badgeRepository.findByCode(badgeCode)
                .orElseThrow(() -> new RuntimeException("徽章不存在"));

        if (userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            return ApiResponse.error(400, "已拥有该徽章");
        }

        UserBadge userBadge = new UserBadge();
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        UserBadge saved = userBadgeRepository.save(userBadge);
        return ApiResponse.success("获得徽章：" + badge.getName(), saved);
    }

    @Override
    @Transactional
    public void checkAndAwardBadges(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Long userId = user.getId();

        // 连续签到5天
        awardIfNotExists(userId, "checkin_5", () -> {
            List<LocalDate> dates = readingCheckInRepository.findCheckInDatesByUserId(userId);
            return calculateConsecutiveDays(dates) >= 5;
        });

        // 连续签到10天
        awardIfNotExists(userId, "checkin_10", () -> {
            List<LocalDate> dates = readingCheckInRepository.findCheckInDatesByUserId(userId);
            return calculateConsecutiveDays(dates) >= 10;
        });

        // 借阅满5本
        awardIfNotExists(userId, "borrow_5", () -> {
            List<BorrowRecord> records = borrowRecordRepository.findByUserAndStatus(user, "RETURNED");
            return records.size() >= 5;
        });

        // 借阅满10本
        awardIfNotExists(userId, "borrow_10", () -> {
            List<BorrowRecord> records = borrowRecordRepository.findByUserAndStatus(user, "RETURNED");
            return records.size() >= 10;
        });

        // 评论被点赞10次
        awardIfNotExists(userId, "review_likes_10", () -> {
            List<BookReview> reviews = bookReviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return reviews.stream().mapToInt(BookReview::getLikeCount).sum() >= 10;
        });

        // 首次评论
        awardIfNotExists(userId, "first_review", () -> {
            return bookReviewRepository.countByUserId(userId) >= 1;
        });

        // 首次借阅
        awardIfNotExists(userId, "first_borrow", () -> {
            List<BorrowRecord> records = borrowRecordRepository.findByUserAndStatus(user, "BORROWING");
            records.addAll(borrowRecordRepository.findByUserAndStatus(user, "RETURNED"));
            return !records.isEmpty();
        });

        // 打卡满30天
        awardIfNotExists(userId, "checkin_total_30", () -> {
            return readingCheckInRepository.countByUserId(userId) >= 30;
        });
    }

    private void awardIfNotExists(Long userId, String badgeCode, java.util.function.Supplier<Boolean> condition) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId,
                badgeRepository.findByCode(badgeCode).map(Badge::getId).orElse(null))) {
            return;
        }
        if (condition.get()) {
            User user = userRepository.findById(userId).orElse(null);
            Badge badge = badgeRepository.findByCode(badgeCode).orElse(null);
            if (user != null && badge != null) {
                UserBadge userBadge = new UserBadge();
                userBadge.setUser(user);
                userBadge.setBadge(badge);
                userBadgeRepository.save(userBadge);
            }
        }
    }

    private int calculateConsecutiveDays(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        Set<LocalDate> dateSet = new HashSet<>(dates);
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate cursor = today;
        if (!dateSet.contains(today)) {
            cursor = today.minusDays(1);
            if (!dateSet.contains(cursor)) return 0;
        }
        for (int i = 0; i < 365; i++) {
            if (dateSet.contains(cursor.minusDays(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}