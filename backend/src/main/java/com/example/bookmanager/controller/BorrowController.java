package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Book;
import com.example.bookmanager.entity.BorrowRecord;
import com.example.bookmanager.entity.ReadingPartner;
import com.example.bookmanager.entity.Reservation;
import com.example.bookmanager.entity.TimeCapsule;
import com.example.bookmanager.repository.BookRepository;
import com.example.bookmanager.repository.BorrowRecordRepository;
import com.example.bookmanager.repository.TimeCapsuleRepository;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.BorrowService;
import com.example.bookmanager.service.ReadingPartnerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    private final BorrowService borrowService;
    private final BorrowRecordRepository borrowRecordRepository;
    private final TimeCapsuleRepository timeCapsuleRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReadingPartnerService readingPartnerService;

    public BorrowController(BorrowService borrowService, BorrowRecordRepository borrowRecordRepository,
                            TimeCapsuleRepository timeCapsuleRepository, BookRepository bookRepository,
                            UserRepository userRepository, ReadingPartnerService readingPartnerService) {
        this.borrowService = borrowService;
        this.borrowRecordRepository = borrowRecordRepository;
        this.timeCapsuleRepository = timeCapsuleRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.readingPartnerService = readingPartnerService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    /**
     * 借阅图书（支持自定义日期）
     */
    @PostMapping("/borrow/{bookId}")
    public ApiResponse<BorrowRecord> borrowBook(@PathVariable Long bookId,
                                                 @RequestBody(required = false) Map<String, String> body,
                                                 HttpServletRequest request) {
        LocalDateTime borrowDate = null;
        LocalDateTime dueDate = null;
        if (body != null) {
            if (body.get("borrowDate") != null && !body.get("borrowDate").isEmpty()) {
                borrowDate = LocalDateTime.parse(body.get("borrowDate"));
            }
            if (body.get("dueDate") != null && !body.get("dueDate").isEmpty()) {
                dueDate = LocalDateTime.parse(body.get("dueDate"));
            }
        }
        return borrowService.borrowBook(bookId, getCurrentUsername(request), borrowDate, dueDate);
    }

    /**
     * 归还图书
     */
    @PostMapping("/return/{borrowId}")
    public ApiResponse<BorrowRecord> returnBook(@PathVariable Long borrowId, HttpServletRequest request) {
        return borrowService.returnBook(borrowId, getCurrentUsername(request));
    }

    /**
     * 支付罚款
     */
    @PostMapping("/pay-fine/{borrowId}")
    public ApiResponse<BorrowRecord> payFine(@PathVariable Long borrowId, HttpServletRequest request) {
        return borrowService.payFine(borrowId, getCurrentUsername(request));
    }

    /**
     * 续借图书
     */
    @PostMapping("/renew/{borrowId}")
    public ApiResponse<BorrowRecord> renewBook(@PathVariable Long borrowId, HttpServletRequest request) {
        return borrowService.renewBook(borrowId, getCurrentUsername(request));
    }

    /**
     * 预约图书
     */
    @PostMapping("/reserve/{bookId}")
    public ApiResponse<Reservation> reserveBook(@PathVariable Long bookId, HttpServletRequest request) {
        return borrowService.reserveBook(bookId, getCurrentUsername(request));
    }

    /**
     * 取消预约
     */
    @PostMapping("/cancel-reservation/{reservationId}")
    public ApiResponse<Void> cancelReservation(@PathVariable Long reservationId, HttpServletRequest request) {
        return borrowService.cancelReservation(reservationId, getCurrentUsername(request));
    }

    /**
     * 我的借阅历史
     */
    @GetMapping("/my-history")
    public ApiResponse<Page<BorrowRecord>> getMyBorrowHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("borrowDate").descending());
        return borrowService.getMyBorrowHistory(getCurrentUsername(request), pageable);
    }

    /**
     * 我的预约
     */
    @GetMapping("/my-reservations")
    public ApiResponse<Page<Reservation>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reserveDate").descending());
        return borrowService.getMyReservations(getCurrentUsername(request), pageable);
    }

    /**
     * 管理员：查看所有借阅记录
     */
    @GetMapping("/all")
    public ApiResponse<Page<BorrowRecord>> getAllBorrows(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("borrowDate").descending());
        return borrowService.getAllBorrows(keyword, pageable);
    }

    /**
     * 管理员：查看所有预约
     */
    @GetMapping("/all-reservations")
    public ApiResponse<Page<Reservation>> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reserveDate").descending());
        return borrowService.getAllReservations(pageable);
    }

    /**
     * 获取某本书最早的归还日期（已借出图书中最早归还的）
     */
    @GetMapping("/earliest-return/{bookId}")
    public ApiResponse<LocalDateTime> getEarliestReturnDate(@PathVariable Long bookId) {
        return borrowService.getEarliestReturnDate(bookId);
    }

    /**
     * 阅读足迹时间轴：按月分组借阅记录
     */
    @GetMapping("/timeline")
    public ApiResponse<Map<String, Object>> getTimeline(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        var user = borrowRecordRepository.findByUserUsernameOrderByBorrowDateDesc(username);
        if (user.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("timeline", new ArrayList<>());
            empty.put("totalBooks", 0);
            empty.put("totalPages", 0);
            empty.put("stackHeight", 0.0);
            return ApiResponse.success(empty);
        }

        // Group by year-month
        Map<String, List<BorrowRecord>> grouped = user.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBorrowDate().getYear() + "-" + String.format("%02d", r.getBorrowDate().getMonthValue()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Map<String, Object>> timeline = new ArrayList<>();
        int totalPages = 0;
        for (Map.Entry<String, List<BorrowRecord>> entry : grouped.entrySet()) {
            Map<String, Object> month = new HashMap<>();
            month.put("month", entry.getKey());
            int monthPages = 0;
            List<Map<String, Object>> items = new ArrayList<>();
            for (BorrowRecord r : entry.getValue()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.getId());
                item.put("bookTitle", r.getBook().getTitle());
                item.put("bookAuthor", r.getBook().getAuthor());
                item.put("borrowDate", r.getBorrowDate());
                item.put("returnDate", r.getReturnDate());
                item.put("status", r.getStatus());
                item.put("pageCount", r.getBook().getPageCount() != null ? r.getBook().getPageCount() : 0);
                items.add(item);
                monthPages += r.getBook().getPageCount() != null ? r.getBook().getPageCount() : 0;
            }
            month.put("items", items);
            month.put("monthPages", monthPages);
            month.put("bookCount", items.size());
            timeline.add(month);
            totalPages += monthPages;
        }

        // Virtual height: assume 100 pages = 1cm
        double stackHeight = Math.round(totalPages * 0.1 * 10.0) / 10.0;

        Map<String, Object> result = new HashMap<>();
        result.put("timeline", timeline);
        result.put("totalBooks", user.size());
        result.put("totalPages", totalPages);
        result.put("stackHeight", stackHeight);
        return ApiResponse.success(result);
    }

    /**
     * 借阅后悔药：5分钟内撤销借阅
     */
    @PostMapping("/cancel/{borrowId}")
    public ApiResponse<?> cancelBorrow(@PathVariable Long borrowId, HttpServletRequest request) {
        String username = getCurrentUsername(request);
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("借阅记录不存在"));
        boolean isAdmin = userRepository.findByUsername(username)
                .map(u -> u.getRole() == com.example.bookmanager.entity.User.Role.ADMIN)
                .orElse(false);
        if (!isAdmin && !record.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能撤销自己的借阅");
        }
        if (!"BORROWING".equals(record.getStatus())) {
            return ApiResponse.error(400, "已归还的借阅记录无法撤销");
        }
        long minutes = ChronoUnit.MINUTES.between(record.getCreatedAt(), LocalDateTime.now());
        if (minutes > 5) {
            return ApiResponse.error(400, "已超过5分钟，无法撤销借阅");
        }
        // 恢复库存
        record.getBook().setAvailableCopies(record.getBook().getAvailableCopies() + 1);
        bookRepository.save(record.getBook());
        // 删除记录
        borrowRecordRepository.delete(record);
        return ApiResponse.success("借阅已撤销", null);
    }

    /**
     * 天气联动荐书
     */
    @GetMapping("/weather-recommend")
    public ApiResponse<Map<String, Object>> weatherRecommend(
            @RequestParam(defaultValue = "") String weather,
            HttpServletRequest request) {
        // 模拟天气：如果没有传参数，随机选一个天气
        String[] weatherTypes = {"晴", "雨", "雪", "云", "风"};
        if (weather == null || weather.isEmpty()) {
            weather = weatherTypes[(int) (Math.random() * weatherTypes.length)];
        }
        List<Book> books = bookRepository.findByWeatherTag(weather);
        Map<String, Object> result = new HashMap<>();
        result.put("weather", weather);
        if (books.isEmpty()) {
            // fallback: 随机推荐
            long count = bookRepository.count();
            if (count > 0) {
                int idx = (int) (Math.random() * count);
                Page<Book> page = bookRepository.findAll(PageRequest.of(idx, 1));
                if (page.hasContent()) {
                    result.put("book", page.getContent().get(0));
                }
            }
        } else {
            result.put("book", books.get((int) (Math.random() * books.size())));
        }
        if (result.containsKey("book")) {
            return ApiResponse.success(result);
        }
        return ApiResponse.error(404, "暂无匹配图书");
    }

    /**
     * 时空胶囊：归还时留言
     */
    @PostMapping("/capsule/{bookId}")
    public ApiResponse<TimeCapsule> leaveCapsule(@PathVariable Long bookId,
                                                  @RequestBody Map<String, String> body,
                                                  HttpServletRequest request) {
        String username = getCurrentUsername(request);
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error(400, "留言内容不能为空");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));
        var user = userRepository.findByUsername(username);
        TimeCapsule capsule = new TimeCapsule();
        capsule.setBook(book);
        user.ifPresent(capsule::setUser);
        capsule.setContent(content.trim());
        return ApiResponse.success("时空胶囊已留下", timeCapsuleRepository.save(capsule));
    }

    /**
     * 时空胶囊：获取某本书的随机一条留言
     */
    @GetMapping("/capsule/{bookId}")
    public ApiResponse<TimeCapsule> getRandomCapsule(@PathVariable Long bookId) {
        List<TimeCapsule> capsules = timeCapsuleRepository.findByBookId(bookId);
        if (capsules.isEmpty()) {
            return ApiResponse.error(404, "暂无时空胶囊留言");
        }
        int idx = (int) (Math.random() * capsules.size());
        return ApiResponse.success(capsules.get(idx));
    }

    /**
     * 今日书架天气预报：获取当天借阅总量
     */
    @GetMapping("/today-stats")
    public ApiResponse<Map<String, Object>> todayStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long count = borrowRecordRepository.countByBorrowDateBetween(todayStart, todayEnd);
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayBorrowCount", count);
        if (count >= 10) {
            stats.put("atmosphere", "hot");
            stats.put("message", "今日图书馆很热闹，大家都在阅读中！");
        } else if (count >= 5) {
            stats.put("atmosphere", "warm");
            stats.put("message", "图书馆里书香弥漫，正是阅读好时光。");
        } else if (count >= 1) {
            stats.put("atmosphere", "calm");
            stats.put("message", "今日适合一个人安静读书，享受独处时光。");
        } else {
            stats.put("atmosphere", "quiet");
            stats.put("message", "图书馆静悄悄的，等你来翻开第一页。");
        }
        return ApiResponse.success(stats);
    }

    /**
     * 今日阅读搭档匹配
     */
    @GetMapping("/partner")
    public ApiResponse<ReadingPartner> getPartner(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        ReadingPartner partner = readingPartnerService.getTodayMatch(username);
        if (partner == null) {
            return ApiResponse.error(404, "今日暂无匹配搭档");
        }
        return ApiResponse.success(partner);
    }
}