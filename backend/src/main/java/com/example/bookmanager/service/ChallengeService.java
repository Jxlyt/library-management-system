package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Challenge;
import com.example.bookmanager.entity.ChallengeCheckIn;
import com.example.bookmanager.entity.ChallengeParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ChallengeService {
    ApiResponse<Challenge> createChallenge(String title, String description, Long bookId, LocalDate startDate, LocalDate endDate, Long badgeId);
    ApiResponse<Challenge> updateChallenge(Long id, String title, String description, Long bookId, LocalDate startDate, LocalDate endDate, Long badgeId);
    ApiResponse<?> deleteChallenge(Long id);
    ApiResponse<Page<Challenge>> getChallenges(Pageable pageable);
    ApiResponse<Challenge> getChallenge(Long challengeId);
    ApiResponse<ChallengeParticipant> joinChallenge(Long challengeId, String username);
    ApiResponse<ChallengeCheckIn> checkIn(Long challengeId, String username, Integer pagesRead, String note);
    ApiResponse<Page<ChallengeCheckIn>> getCheckIns(Long participantId, Pageable pageable);
    ApiResponse<Page<ChallengeParticipant>> getMyChallenges(String username, Pageable pageable);
    ApiResponse<Page<ChallengeParticipant>> getChallengeParticipants(Long challengeId, Pageable pageable);
}