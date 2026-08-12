package com.example.bookmanager.repository;

import com.example.bookmanager.entity.Reservation;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUserOrderByReserveDateDesc(User user, Pageable pageable);

    List<Reservation> findByBookIdAndStatus(Long bookId, String status);

    Optional<Reservation> findByBookIdAndUserIdAndStatus(Long bookId, Long userId, String status);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    Page<Reservation> findAllByOrderByReserveDateDesc(Pageable pageable);
}