package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.BadgeService;
import com.example.bookmanager.service.BorrowService;
import com.example.bookmanager.service.PointService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BorrowServiceImpl implements BorrowService {

    private static final int BORROW_DAYS = 30;
    private static final int MAX_BORROW_COUNT = 10;
    private static final int MAX_RENEW_COUNT = 2;
    private static final int RENEW_DAYS = 15;
    private static final double FINE_PER_DAY = 0.5;
    private static final int RESERVE_EXPIRE_DAYS = 7;

    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final PointService pointService;

    public BorrowServiceImpl(BorrowRecordRepository borrowRecordRepository,
                             ReservationRepository reservationRepository,
                             BookRepository bookRepository,
                             UserRepository userRepository,
                             BadgeService badgeService,
                             PointService pointService) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.badgeService = badgeService;
        this.pointService = pointService;
    }

    @Override
    @Transactional
    public ApiResponse<BorrowRecord> borrowBook(Long bookId, String username, LocalDateTime borrowDate, LocalDateTime dueDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        if (book.getAvailableCopies() <= 0) {
            return ApiResponse.error(400, "该图书已全部借出，可进行预约");
        }

        // 检查是否已借阅同一本书且未归还
        List<BorrowRecord> existingBorrows = borrowRecordRepository.findByBookIdAndStatus(bookId, "BORROWING");
        boolean alreadyBorrowed = existingBorrows.stream()
                .anyMatch(r -> r.getUser().getId().equals(user.getId()));
        if (alreadyBorrowed) {
            return ApiResponse.error(400, "您已借阅该书，请勿重复借阅");
        }

        // 检查是否已借阅超过5本书
        List<BorrowRecord> userBorrowing = borrowRecordRepository.findByUserAndStatus(user, "BORROWING");
        if (userBorrowing.size() >= MAX_BORROW_COUNT) {
            return ApiResponse.error(400, "您已借阅" + MAX_BORROW_COUNT + "本书，达到上限");
        }

        // 使用自定义日期或默认日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime borrowDt = borrowDate != null ? borrowDate : now;
        LocalDateTime dueDt = dueDate != null ? dueDate : now.plusDays(BORROW_DAYS);

        if (dueDt.isBefore(borrowDt)) {
            return ApiResponse.error(400, "归还日期不能早于借阅日期");
        }

        // 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setBook(book);
        record.setUser(user);
        record.setBorrowDate(borrowDt);
        record.setDueDate(dueDt);
        record.setStatus("BORROWING");

        // 更新库存
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        if (book.getAvailableCopies() == 0) {
            book.setStatus("BORROWED");
        }
        bookRepository.save(book);

        // 自动完成该用户的预约（如果有）
        reservationRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "PENDING")
                .ifPresent(reservation -> {
                    reservation.setStatus("FULFILLED");
                    reservationRepository.save(reservation);
                });

        BorrowRecord saved = borrowRecordRepository.save(record);
        badgeService.checkAndAwardBadges(username);
        pointService.addPoints(username, 10, "借阅图书");
        return ApiResponse.success("借阅成功，应还日期：" + saved.getDueDate().toLocalDate(), saved);
    }

    private boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == User.Role.ADMIN)
                .orElse(false);
    }

    @Override
    @Transactional
    public ApiResponse<BorrowRecord> returnBook(Long borrowId, String username) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("借阅记录不存在"));

        boolean admin = isAdmin(username);
        if (!admin && !record.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能归还自己的借阅");
        }

        if (!"BORROWING".equals(record.getStatus())) {
            return ApiResponse.error(400, "该记录已归还");
        }

        // 计算逾期罚款（仅当罚款未支付时重新计算，防止重复计算）
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(record.getDueDate()) && !Boolean.TRUE.equals(record.getFinePaid())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), now);
            double fine = overdueDays * FINE_PER_DAY;
            record.setFine(fine);
        }

        // 如果有逾期罚款且未支付，不允许归还
        if (record.getFine() > 0 && !Boolean.TRUE.equals(record.getFinePaid())) {
            return ApiResponse.error(400, "您有逾期罚款 ¥" + String.format("%.1f", record.getFine()) + " 未支付，请先支付罚款再归还");
        }

        record.setReturnDate(now);
        record.setStatus("RETURNED");

        // 更新库存（不超过 totalCopies）
        Book book = record.getBook();
        int newAvailable = book.getAvailableCopies() + 1;
        if (newAvailable > book.getTotalCopies()) {
            newAvailable = book.getTotalCopies();
        }
        book.setAvailableCopies(newAvailable);
        if (book.getAvailableCopies() > 0 && "BORROWED".equals(book.getStatus())) {
            book.setStatus("AVAILABLE");
        }
        bookRepository.save(book);

        BorrowRecord saved = borrowRecordRepository.save(record);

        String msg = "归还成功";
        if (saved.getFine() > 0) {
            msg += "，逾期罚款 ¥" + String.format("%.1f", saved.getFine());
        }
        pointService.addPoints(username, 5, "归还图书");
        return ApiResponse.success(msg, saved);
    }

    @Override
    @Transactional
    public ApiResponse<BorrowRecord> payFine(Long borrowId, String username) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("借阅记录不存在"));

        boolean admin = isAdmin(username);
        if (!admin && !record.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能支付自己的罚款");
        }

        if (!"BORROWING".equals(record.getStatus())) {
            return ApiResponse.error(400, "该记录已归还");
        }

        if (Boolean.TRUE.equals(record.getFinePaid())) {
            return ApiResponse.error(400, "罚款已支付");
        }

        // 先计算罚款（如果尚未计算）
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), now);
            if (overdueDays > 0) {
                double fine = overdueDays * FINE_PER_DAY;
                record.setFine(fine);
            }
        }

        if (record.getFine() <= 0) {
            return ApiResponse.error(400, "没有逾期罚款");
        }

        record.setFinePaid(true);
        BorrowRecord saved = borrowRecordRepository.save(record);
        return ApiResponse.success("罚款支付成功，可以归还了", saved);
    }

    @Override
    @Transactional
    public ApiResponse<BorrowRecord> renewBook(Long borrowId, String username) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("借阅记录不存在"));

        boolean admin = isAdmin(username);
        if (!admin && !record.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能续借自己的借阅");
        }

        if (!"BORROWING".equals(record.getStatus())) {
            return ApiResponse.error(400, "只能续借借阅中的图书");
        }

        if (record.getRenewCount() >= MAX_RENEW_COUNT) {
            return ApiResponse.error(400, "续借次数已达上限（" + MAX_RENEW_COUNT + "次）");
        }

        // 检查是否有其他用户预约了此书
        List<Reservation> pendingReservations = reservationRepository.findByBookIdAndStatus(
                record.getBook().getId(), "PENDING");
        if (!pendingReservations.isEmpty()) {
            return ApiResponse.error(400, "该图书已被其他用户预约，无法续借");
        }

        record.setRenewCount(record.getRenewCount() + 1);
        record.setDueDate(LocalDateTime.now().plusDays(RENEW_DAYS));
        record.setStatus("BORROWING");

        BorrowRecord saved = borrowRecordRepository.save(record);
        return ApiResponse.success("续借成功，新的应还日期：" + saved.getDueDate().toLocalDate(), saved);
    }

    @Override
    @Transactional
    public ApiResponse<Reservation> reserveBook(Long bookId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        // 如果还有库存，直接提示可借阅
        if (book.getAvailableCopies() > 0) {
            return ApiResponse.error(400, "该图书还有库存，可直接借阅");
        }

        // 检查是否已预约
        boolean alreadyReserved = reservationRepository
                .findByBookIdAndUserIdAndStatus(bookId, user.getId(), "PENDING")
                .isPresent();
        if (alreadyReserved) {
            return ApiResponse.error(400, "您已预约该书");
        }

        // 检查是否已借阅该书且未归还
        List<BorrowRecord> existingBorrows = borrowRecordRepository.findByBookIdAndStatus(bookId, "BORROWING");
        boolean alreadyBorrowed = existingBorrows.stream()
                .anyMatch(r -> r.getUser().getId().equals(user.getId()));
        if (alreadyBorrowed) {
            return ApiResponse.error(400, "您已借阅该书，无需预约");
        }

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setReserveDate(LocalDateTime.now());
        reservation.setExpireDate(LocalDateTime.now().plusDays(RESERVE_EXPIRE_DAYS));
        reservation.setStatus("PENDING");

        Reservation saved = reservationRepository.save(reservation);
        return ApiResponse.success("预约成功，请在图书归还后" + RESERVE_EXPIRE_DAYS + "天内借阅", saved);
    }

    @Override
    @Transactional
    public ApiResponse<Void> cancelReservation(Long reservationId, String username) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预约记录不存在"));

        boolean admin = isAdmin(username);
        if (!admin && !reservation.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能取消自己的预约");
        }

        if (!"PENDING".equals(reservation.getStatus())) {
            return ApiResponse.error(400, "该预约已处理");
        }

        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);
        return ApiResponse.success("取消预约成功", null);
    }

    @Override
    public ApiResponse<Page<BorrowRecord>> getMyBorrowHistory(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Page<BorrowRecord> records = borrowRecordRepository.findByUserOrderByBorrowDateDesc(user, pageable);
        return ApiResponse.success(records);
    }

    @Override
    public ApiResponse<Page<BorrowRecord>> getAllBorrows(String keyword, Pageable pageable) {
        Page<BorrowRecord> records;
        if (StringUtils.hasText(keyword)) {
            records = borrowRecordRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            records = borrowRecordRepository.findAllByOrderByBorrowDateDesc(pageable);
        }
        return ApiResponse.success(records);
    }

    @Override
    public ApiResponse<Page<Reservation>> getMyReservations(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Page<Reservation> reservations = reservationRepository.findByUserOrderByReserveDateDesc(user, pageable);
        return ApiResponse.success(reservations);
    }

    @Override
    public ApiResponse<Page<Reservation>> getAllReservations(Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findAllByOrderByReserveDateDesc(pageable);
        return ApiResponse.success(reservations);
    }

    @Override
    public ApiResponse<LocalDateTime> getEarliestReturnDate(Long bookId) {
        List<BorrowRecord> records = borrowRecordRepository.findByBookIdAndStatus(bookId, "BORROWING");
        if (records.isEmpty()) {
            return ApiResponse.success(null);
        }
        LocalDateTime earliest = records.stream()
                .map(BorrowRecord::getDueDate)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return ApiResponse.success(earliest);
    }
}