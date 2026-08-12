package com.example.bookmanager.repository;

import com.example.bookmanager.entity.ChallengeCheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeCheckInRepository extends JpaRepository<ChallengeCheckIn, Long> {
    Optional<ChallengeCheckIn> findByParticipantIdAndCheckInDate(Long participantId, LocalDate checkInDate);
    List<ChallengeCheckIn> findByParticipantIdOrderByCheckInDateDesc(Long participantId);
    Page<ChallengeCheckIn> findByParticipantIdOrderByCheckInDateDesc(Long participantId, Pageable pageable);
    long countByParticipantId(Long participantId);
}