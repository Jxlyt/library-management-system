package com.example.bookmanager.repository;

import com.example.bookmanager.entity.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Order> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);
    Page<Order> findByStatus(String status, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findByStatusFilter(@Param("status") String status, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.payAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'RECEIVED', 'COMPLETED') AND o.createdAt BETWEEN :start AND :end")
    double sumPayAmountByPaidAndCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT oi.bookId, SUM(oi.quantity) as cnt FROM OrderItem oi JOIN oi.order o WHERE o.status IN ('PAID', 'SHIPPED', 'RECEIVED', 'COMPLETED') GROUP BY oi.bookId ORDER BY cnt DESC")
    List<Object[]> findTopSellingBooks(Pageable pageable);
}