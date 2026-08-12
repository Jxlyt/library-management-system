package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.ChallengeService;
import com.example.bookmanager.service.PointService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final ChallengeCheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointService pointService;

    public ChallengeServiceImpl(ChallengeRepository challengeRepository,
                                ChallengeParticipantRepository participantRepository,
                                ChallengeCheckInRepository checkInRepository,
                                UserRepository userRepository,
                                BadgeRepository badgeRepository,
                                UserBadgeRepository userBadgeRepository,
                                PointService pointService) {
        this.challengeRepository = challengeRepository;
        this.participantRepository = participantRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.pointService = pointService;
    }

    @Override
    @Transactional
    public ApiResponse<Challenge> createChallenge(String title, String description, Long bookId, LocalDate startDate, LocalDate endDate, Long badgeId) {
        Challenge challenge = new Challenge();
        challenge.setTitle(title);
        challenge.setDescription(description);
        challenge.setBookId(bookId);
        challenge.setStartDate(startDate);
        challenge.setEndDate(endDate);
        challenge.setBadgeId(badgeId);
        return ApiResponse.success("创建成功", challengeRepository.save(challenge));
    }

    @Override
    @Transactional
    public ApiResponse<Challenge> updateChallenge(Long id, String title, String description, Long bookId, LocalDate startDate, LocalDate endDate, Long badgeId) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        if (title != null) challenge.setTitle(title);
        if (description != null) challenge.setDescription(description);
        if (bookId != null) challenge.setBookId(bookId);
        if (startDate != null) challenge.setStartDate(startDate);
        if (endDate != null) challenge.setEndDate(endDate);
        if (badgeId != null) challenge.setBadgeId(badgeId);
        return ApiResponse.success("更新成功", challengeRepository.save(challenge));
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }

    @Override
    public ApiResponse<Page<Challenge>> getChallenges(Pageable pageable) {
        return ApiResponse.success(challengeRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @Override
    public ApiResponse<Challenge> getChallenge(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        return ApiResponse.success(challenge);
    }

    @Override
    @Transactional
    public ApiResponse<ChallengeParticipant> joinChallenge(Long challengeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        if (participantRepository.findByChallengeIdAndUserId(challengeId, user.getId()).isPresent()) {
            return ApiResponse.error(400, "已报名该活动");
        }

        ChallengeParticipant participant = new ChallengeParticipant();
        participant.setChallenge(challenge);
        participant.setUser(user);
        ChallengeParticipant saved = participantRepository.save(participant);
        challenge.setParticipantCount(challenge.getParticipantCount() + 1);
        challengeRepository.save(challenge);
        return ApiResponse.success("报名成功", saved);
    }

    @Override
    @Transactional
    public ApiResponse<ChallengeCheckIn> checkIn(Long challengeId, String username, Integer pagesRead, String note) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ChallengeParticipant participant = participantRepository.findByChallengeIdAndUserId(challengeId, user.getId())
                .orElseThrow(() -> new RuntimeException("请先报名活动"));

        LocalDate today = LocalDate.now();
        if (checkInRepository.findByParticipantIdAndCheckInDate(participant.getId(), today).isPresent()) {
            return ApiResponse.error(400, "今天已打卡");
        }

        ChallengeCheckIn checkIn = new ChallengeCheckIn();
        checkIn.setParticipant(participant);
        checkIn.setCheckInDate(today);
        checkIn.setPagesRead(pagesRead);
        checkIn.setNote(note);
        ChallengeCheckIn saved = checkInRepository.save(checkIn);

        participant.setProgress(participant.getProgress() + 1);
        participantRepository.save(participant);

        pointService.addPoints(username, 5, "阅读打卡");

        // 检查是否完成活动
        Challenge challenge = participant.getChallenge();
        if (today.isAfter(challenge.getEndDate()) || today.equals(challenge.getEndDate())) {
            long checkInDays = checkInRepository.countByParticipantId(participant.getId());
            long totalDays = challenge.getStartDate().until(challenge.getEndDate()).getDays() + 1;
            if (checkInDays >= totalDays * 0.8) {
                participant.setCompleted(true);
                participantRepository.save(participant);
                // 颁发徽章
                if (challenge.getBadgeId() != null) {
                    badgeRepository.findById(challenge.getBadgeId()).ifPresent(badge -> {
                        if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                            UserBadge ub = new UserBadge();
                            ub.setUser(user);
                            ub.setBadge(badge);
                            userBadgeRepository.save(ub);
                        }
                    });
                }
            }
        }

        return ApiResponse.success("打卡成功", saved);
    }

    @Override
    public ApiResponse<Page<ChallengeCheckIn>> getCheckIns(Long participantId, Pageable pageable) {
        return ApiResponse.success(checkInRepository.findByParticipantIdOrderByCheckInDateDesc(participantId, pageable));
    }

    @Override
    public ApiResponse<Page<ChallengeParticipant>> getMyChallenges(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(participantRepository.findByUserIdOrderByJoinedAtDesc(user.getId(), pageable));
    }

    @Override
    public ApiResponse<Page<ChallengeParticipant>> getChallengeParticipants(Long challengeId, Pageable pageable) {
        return ApiResponse.success(participantRepository.findByChallengeIdOrderByJoinedAtDesc(challengeId, pageable));
    }
}