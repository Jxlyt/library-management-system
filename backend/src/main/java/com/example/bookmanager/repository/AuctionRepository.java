package com.example.bookmanager.repository;

import com.example.bookmanager.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query(value = "SELECT DISTINCT a FROM Auction a LEFT JOIN FETCH a.book LEFT JOIN FETCH a.winner WHERE a.status = :status ORDER BY a.endTime ASC",
           countQuery = "SELECT COUNT(a) FROM Auction a WHERE a.status = :status")
    Page<Auction> findByStatusOrderByEndTimeAsc(@Param("status") String status, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Auction a LEFT JOIN FETCH a.book LEFT JOIN FETCH a.winner WHERE a.status IN :statuses ORDER BY a.endTime DESC",
           countQuery = "SELECT COUNT(a) FROM Auction a WHERE a.status IN :statuses")
    Page<Auction> findByStatusInOrderByEndTimeDesc(@Param("statuses") List<String> statuses, Pageable pageable);

    @Query(value = "SELECT DISTINCT a FROM Auction a LEFT JOIN FETCH a.book LEFT JOIN FETCH a.winner ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(a) FROM Auction a")
    Page<Auction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Auction a LEFT JOIN FETCH a.book LEFT JOIN FETCH a.winner WHERE a.id = :id")
    java.util.Optional<Auction> findByIdWithDetails(@Param("id") Long id);

    List<Auction> findByStatus(String status);
    List<Auction> findByStatusAndEndTimeBefore(String status, java.time.LocalDateTime time);
}