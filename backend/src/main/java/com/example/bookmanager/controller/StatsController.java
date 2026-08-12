package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.repository.BookRepository;
import com.example.bookmanager.repository.BorrowRecordRepository;
import com.example.bookmanager.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public StatsController(BookRepository bookRepository,
                           UserRepository userRepository,
                           BorrowRecordRepository borrowRecordRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    /**
     * 核心指标统计
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalBooks", bookRepository.count());
        data.put("totalUsers", userRepository.count());
        data.put("activeUsers", userRepository.countByStatus("ACTIVE"));

        long totalBorrows = borrowRecordRepository.count();
        data.put("totalBorrows", totalBorrows);

        // 今日借阅量（基于记录创建时间）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayBorrows = borrowRecordRepository.countByCreatedAtAfter(todayStart);
        data.put("todayBorrows", todayBorrows);

        // 当前借出数
        long currentBorrowing = borrowRecordRepository.countByStatus("BORROWING");
        data.put("currentBorrowing", currentBorrowing);

        return ApiResponse.success(data);
    }

    /**
     * 图书借阅排行榜
     */
    @GetMapping("/borrow-ranking")
    public ApiResponse<List<Map<String, Object>>> borrowRanking() {
        List<Object[]> rows = borrowRecordRepository.findBorrowRanking();
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(rows.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 本月借阅排行榜
     */
    @GetMapping("/monthly-ranking")
    public ApiResponse<List<Map<String, Object>>> monthlyRanking() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Object[]> rows = borrowRecordRepository.findMonthlyBorrowRanking(startOfMonth);
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(rows.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", row[0]);
            item.put("title", row[1]);
            item.put("author", row[2]);
            item.put("category", row[3]);
            item.put("count", row[4]);
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取所有分类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(bookRepository.findAllCategories());
    }

    /**
     * 分类统计
     */
    @GetMapping("/category-stats")
    public ApiResponse<List<Map<String, Object>>> categoryStats() {
        List<Object[]> rows = bookRepository.findCategoryStats();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return ApiResponse.success(result);
    }
}