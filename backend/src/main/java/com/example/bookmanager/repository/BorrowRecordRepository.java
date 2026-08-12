package com.example.bookmanager.repository;

import com.example.bookmanager.entity.BorrowRecord;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    Page<BorrowRecord> findByUserOrderByBorrowDateDesc(User user, Pageable pageable);

    List<BorrowRecord> findByUserUsernameOrderByBorrowDateDesc(String username);

    List<BorrowRecord> findByUserAndStatus(User user, String status);

    long countByBorrowDateBetween(LocalDateTime start, LocalDateTime end);

    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);

    Optional<BorrowRecord> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT b FROM BorrowRecord b WHERE b.status = 'BORROWING' AND b.dueDate < CURRENT_TIMESTAMP")
    List<BorrowRecord> findOverdueRecords();

    Page<BorrowRecord> findAllByOrderByBorrowDateDesc(Pageable pageable);

    @Query("SELECT b FROM BorrowRecord b WHERE " +
           "LOWER(b.book.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<BorrowRecord> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByBorrowDateAfter(LocalDateTime dateTime);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByStatus(String status);

    @Query("SELECT b.book.title, COUNT(b) as cnt FROM BorrowRecord b GROUP BY b.book.title ORDER BY cnt DESC")
    List<Object[]> findBorrowRanking();

    @Query("SELECT b.book.id, b.book.title, b.book.author, b.book.category, COUNT(b) as cnt FROM BorrowRecord b WHERE b.borrowDate >= :startDate GROUP BY b.book.id, b.book.title, b.book.author, b.book.category ORDER BY cnt DESC")
    List<Object[]> findMonthlyBorrowRanking(@Param("startDate") LocalDateTime startDate);
}