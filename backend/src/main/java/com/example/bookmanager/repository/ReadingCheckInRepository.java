package com.example.bookmanager.repository;

import com.example.bookmanager.entity.ReadingCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingCheckInRepository extends JpaRepository<ReadingCheckIn, Long> {

    Optional<ReadingCheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    List<ReadingCheckIn> findByUserIdOrderByCheckInDateDesc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT r FROM ReadingCheckIn r WHERE r.user.id = :userId AND r.checkInDate >= :startDate ORDER BY r.checkInDate DESC")
    List<ReadingCheckIn> findRecentByUserId(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    @Query("SELECT r.checkInDate FROM ReadingCheckIn r WHERE r.user.id = :userId ORDER BY r.checkInDate ASC")
    List<LocalDate> findCheckInDatesByUserId(@Param("userId") Long userId);
}