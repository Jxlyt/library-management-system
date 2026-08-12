package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.BadgeService;
import com.example.bookmanager.service.PointService;
import com.example.bookmanager.service.SocialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialServiceImpl implements SocialService {

    private final FavoriteRepository favoriteRepository;
    private final BookReviewRepository bookReviewRepository;
    private final ReadingNoteRepository readingNoteRepository;
    private final ReadingCheckInRepository readingCheckInRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserFollowRepository userFollowRepository;
    private final ActivityRepository activityRepository;
    private final BadgeService badgeService;
    private final PointService pointService;

    public SocialServiceImpl(FavoriteRepository favoriteRepository,
                             BookReviewRepository bookReviewRepository,
                             ReadingNoteRepository readingNoteRepository,
                             ReadingCheckInRepository readingCheckInRepository,
                             BookRepository bookRepository,
                             UserRepository userRepository,
                             BorrowRecordRepository borrowRecordRepository,
                             UserFollowRepository userFollowRepository,
                             ActivityRepository activityRepository,
                             BadgeService badgeService,
                             PointService pointService) {
        this.favoriteRepository = favoriteRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.readingNoteRepository = readingNoteRepository;
        this.readingCheckInRepository = readingCheckInRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userFollowRepository = userFollowRepository;
        this.activityRepository = activityRepository;
        this.badgeService = badgeService;
        this.pointService = pointService;
    }

    // ======== 收藏 ========

    @Override
    @Transactional
    public ApiResponse<?> addFavorite(Long bookId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        if (favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            return ApiResponse.error(400, "已收藏该图书");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);
        favoriteRepository.save(favorite);

        recordActivity(user, "FAVORITE", "BOOK", bookId, "收藏了《" + book.getTitle() + "》");
        badgeService.checkAndAwardBadges(username);
        return ApiResponse.success("收藏成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> removeFavorite(Long bookId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        favoriteRepository.findByUserIdAndBookId(user.getId(), bookId)
                .ifPresentOrElse(
                        f -> favoriteRepository.delete(f),
                        () -> { throw new RuntimeException("未收藏该图书"); }
                );
        return ApiResponse.success("已取消收藏", null);
    }

    @Override
    public ApiResponse<Page<Favorite>> getMyFavorites(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Page<Favorite> favorites = favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return ApiResponse.success(favorites);
    }

    @Override
    public ApiResponse<Boolean> isFavorited(Long bookId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        boolean favorited = favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId);
        return ApiResponse.success(favorited);
    }

    // ======== 评价 ========

    @Override
    @Transactional
    public ApiResponse<BookReview> addReview(Long bookId, String username, Integer rating, String comment) {
        return addFullReview(bookId, username, null, comment, rating);
    }

    @Override
    @Transactional
    public ApiResponse<BookReview> addFullReview(Long bookId, String username, String title, String content, Integer rating) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        if (rating == null || rating < 1 || rating > 5) {
            return ApiResponse.error(400, "评分必须在1-5之间");
        }

        if (bookReviewRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            return ApiResponse.error(400, "您已评价过该书");
        }

        BookReview review = new BookReview();
        review.setUser(user);
        review.setBook(book);
        review.setTitle(title);
        review.setContent(content);
        review.setComment(content);
        review.setRating(rating);
        BookReview saved = bookReviewRepository.save(review);

        recordActivity(user, "REVIEW", "BOOK", bookId, "评价了《" + book.getTitle() + "》");
        badgeService.checkAndAwardBadges(username);
        return ApiResponse.success("评价成功", saved);
    }

    @Override
    public ApiResponse<Page<BookReview>> getBookReviews(Long bookId, Pageable pageable) {
        Page<BookReview> reviews = bookReviewRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable);
        return ApiResponse.success(reviews);
    }

    @Override
    public ApiResponse<Map<String, Object>> getBookRatingStats(Long bookId) {
        Map<String, Object> stats = new HashMap<>();
        Double avgRating = bookReviewRepository.findAverageRatingByBookId(bookId);
        long reviewCount = bookReviewRepository.countByBookId(bookId);
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        stats.put("reviewCount", reviewCount);
        return ApiResponse.success(stats);
    }

    @Override
    @Transactional
    public ApiResponse<?> likeReview(Long reviewId, String username) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        review.setLikeCount(review.getLikeCount() + 1);
        bookReviewRepository.save(review);
        badgeService.checkAndAwardBadges(review.getUser().getUsername());
        return ApiResponse.success("点赞成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> pinReview(Long reviewId) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        review.setIsPinned(true);
        bookReviewRepository.save(review);
        return ApiResponse.success("置顶成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> unpinReview(Long reviewId) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        review.setIsPinned(false);
        bookReviewRepository.save(review);
        return ApiResponse.success("取消置顶成功", null);
    }

    @Override
    public ApiResponse<Page<BookReview>> getPinnedReviews(Pageable pageable) {
        return ApiResponse.success(bookReviewRepository.findPinnedReviews(pageable));
    }

    // ======== 读书笔记 ========

    @Override
    @Transactional
    public ApiResponse<ReadingNote> createNote(Long bookId, String username, String title, String content, Boolean isPublic) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error(400, "笔记标题不能为空");
        }

        ReadingNote note = new ReadingNote();
        note.setUser(user);
        note.setBook(book);
        note.setTitle(title);
        note.setContent(content);
        note.setIsPublic(isPublic != null ? isPublic : true);
        ReadingNote saved = readingNoteRepository.save(note);

        recordActivity(user, "NOTE", "BOOK", bookId, "写了读书笔记《" + title + "》");
        return ApiResponse.success("笔记创建成功", saved);
    }

    @Override
    @Transactional
    public ApiResponse<ReadingNote> updateNote(Long noteId, String username, String title, String content, Boolean isPublic) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ReadingNote note = readingNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));

        if (!note.getUser().getId().equals(user.getId())) {
            return ApiResponse.error(403, "只能修改自己的笔记");
        }

        if (title != null && !title.trim().isEmpty()) note.setTitle(title);
        if (content != null) note.setContent(content);
        if (isPublic != null) note.setIsPublic(isPublic);
        return ApiResponse.success("笔记更新成功", readingNoteRepository.save(note));
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteNote(Long noteId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ReadingNote note = readingNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));
        if (!note.getUser().getId().equals(user.getId())) {
            return ApiResponse.error(403, "只能删除自己的笔记");
        }
        readingNoteRepository.delete(note);
        return ApiResponse.success("笔记已删除", null);
    }

    @Override
    public ApiResponse<Page<ReadingNote>> getMyNotes(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(readingNoteRepository.findByUserIdOrderByUpdatedAtDesc(user.getId(), pageable));
    }

    @Override
    public ApiResponse<Page<ReadingNote>> getPublicNotes(Pageable pageable) {
        return ApiResponse.success(readingNoteRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable));
    }

    // ======== 阅读打卡 ========

    @Override
    @Transactional
    public ApiResponse<?> checkIn(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        LocalDate today = LocalDate.now();
        if (readingCheckInRepository.findByUserIdAndCheckInDate(user.getId(), today).isPresent()) {
            return ApiResponse.error(400, "今天已打卡");
        }

        ReadingCheckIn checkIn = new ReadingCheckIn();
        checkIn.setUser(user);
        checkIn.setCheckInDate(today);
        readingCheckInRepository.save(checkIn);

        recordActivity(user, "CHECKIN", null, null, "完成了今日阅读打卡");
        badgeService.checkAndAwardBadges(username);

        int streak = calculateStreak(user.getId());
        return ApiResponse.success("打卡成功！当前连续打卡 " + streak + " 天", streak);
    }

    @Override
    public ApiResponse<Map<String, Object>> getCheckInStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCheckIns", readingCheckInRepository.countByUserId(user.getId()));
        stats.put("currentStreak", calculateStreak(user.getId()));
        stats.put("todayChecked", readingCheckInRepository
                .findByUserIdAndCheckInDate(user.getId(), LocalDate.now()).isPresent());
        return ApiResponse.success(stats);
    }

    // ======== 关注 ========

    @Override
    @Transactional
    public ApiResponse<?> followUser(Long followeeId, String username) {
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("目标用户不存在"));

        if (follower.getId().equals(followeeId)) {
            return ApiResponse.error(400, "不能关注自己");
        }
        if (userFollowRepository.existsByFollowerIdAndFolloweeId(follower.getId(), followeeId)) {
            return ApiResponse.error(400, "已关注该用户");
        }

        UserFollow follow = new UserFollow();
        follow.setFollower(follower);
        follow.setFollowee(followee);
        userFollowRepository.save(follow);
        return ApiResponse.success("关注成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> unfollowUser(Long followeeId, String username) {
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        userFollowRepository.deleteByFollowerIdAndFolloweeId(follower.getId(), followeeId);
        return ApiResponse.success("已取消关注", null);
    }

    @Override
    public ApiResponse<Page<UserFollow>> getFollowers(Long userId, Pageable pageable) {
        return ApiResponse.success(userFollowRepository.findByFolloweeIdOrderByCreatedAtDesc(userId, pageable));
    }

    @Override
    public ApiResponse<Page<UserFollow>> getFollowing(Long userId, Pageable pageable) {
        return ApiResponse.success(userFollowRepository.findByFollowerIdOrderByCreatedAtDesc(userId, pageable));
    }

    @Override
    public ApiResponse<Boolean> isFollowing(Long userId, String username) {
        User follower = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(userFollowRepository.existsByFollowerIdAndFolloweeId(follower.getId(), userId));
    }

    @Override
    public ApiResponse<Map<String, Object>> getFollowStats(Long userId, String username) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("followerCount", userFollowRepository.countByFolloweeId(userId));
        stats.put("followingCount", userFollowRepository.countByFollowerId(userId));
        if (username != null) {
            User follower = userRepository.findByUsername(username).orElse(null);
            if (follower != null) {
                stats.put("isFollowing", userFollowRepository.existsByFollowerIdAndFolloweeId(follower.getId(), userId));
            }
        }
        return ApiResponse.success(stats);
    }

    // ======== 动态 ========

    @Override
    public ApiResponse<Page<Activity>> getUserActivities(Long userId, Pageable pageable) {
        return ApiResponse.success(activityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }

    @Override
    public ApiResponse<Page<Activity>> getFollowedActivities(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Page<UserFollow> followings = userFollowRepository.findByFollowerIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged());
        List<Long> followeeIds = followings.getContent().stream()
                .map(f -> f.getFollowee().getId())
                .collect(Collectors.toList());
        if (followeeIds.isEmpty()) {
            return ApiResponse.success(Page.empty());
        }
        return ApiResponse.success(activityRepository.findByUserIdInOrderByCreatedAtDesc(followeeIds, pageable));
    }

    // ======== 私有方法 ========

    private void recordActivity(User user, String type, String targetType, Long targetId, String summary) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setType(type);
        activity.setTargetType(targetType);
        activity.setTargetId(targetId);
        activity.setSummary(summary);
        activityRepository.save(activity);
    }

    private int calculateStreak(Long userId) {
        List<LocalDate> dates = readingCheckInRepository.findCheckInDatesByUserId(userId);
        if (dates.isEmpty()) return 0;

        Set<LocalDate> dateSet = new HashSet<>(dates);
        LocalDate today = LocalDate.now();
        if (!dateSet.contains(today)) {
            LocalDate yesterday = today.minusDays(1);
            if (!dateSet.contains(yesterday)) return 0;
        }

        int streak = 1;
        LocalDate cursor = dateSet.contains(today) ? today : today.minusDays(1);
        for (int i = 1; i <= 365; i++) {
            if (dateSet.contains(cursor.minusDays(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}