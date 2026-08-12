package com.example.bookmanager.repository;

import com.example.bookmanager.entity.ChallengeParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {
    Page<ChallengeParticipant> findByChallengeIdOrderByJoinedAtDesc(Long challengeId, Pageable pageable);
    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);
    List<ChallengeParticipant> findByUserIdAndCompletedFalse(Long userId);
    Page<ChallengeParticipant> findByUserIdOrderByJoinedAtDesc(Long userId, Pageable pageable);
}