package com.example.bookmanager.repository;

import com.example.bookmanager.entity.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    Page<BookReview> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    Optional<BookReview> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT AVG(r.rating) FROM BookReview r WHERE r.book.id = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Long bookId);

    long countByBookId(Long bookId);
    long countByUserId(Long userId);
    List<BookReview> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Query("SELECT r FROM BookReview r WHERE r.isPinned = true ORDER BY r.createdAt DESC")
    Page<BookReview> findPinnedReviews(Pageable pageable);
}