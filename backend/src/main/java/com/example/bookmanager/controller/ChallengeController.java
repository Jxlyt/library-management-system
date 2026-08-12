package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Challenge;
import com.example.bookmanager.entity.ChallengeCheckIn;
import com.example.bookmanager.entity.ChallengeParticipant;
import com.example.bookmanager.service.ChallengeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping
    public ApiResponse<Challenge> createChallenge(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        Long bookId = parseLong(body.get("bookId"));
        LocalDate startDate = parseDate(body.get("startDate"));
        LocalDate endDate = parseDate(body.get("endDate"));
        Long badgeId = parseLong(body.get("badgeId"));
        return challengeService.createChallenge(title, description, bookId, startDate, endDate, badgeId);
    }

    @PutMapping("/{id}")
    public ApiResponse<Challenge> updateChallenge(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        Long bookId = parseLong(body.get("bookId"));
        LocalDate startDate = parseDate(body.get("startDate"));
        LocalDate endDate = parseDate(body.get("endDate"));
        Long badgeId = parseLong(body.get("badgeId"));
        return challengeService.updateChallenge(id, title, description, bookId, startDate, endDate, badgeId);
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String && !((String) obj).isEmpty()) {
            try { return Long.parseLong((String) obj); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private LocalDate parseDate(Object dateObj) {
        if (dateObj == null) return null;
        String dateStr = (String) dateObj;
        if (dateStr.contains("T")) {
            dateStr = dateStr.substring(0, dateStr.indexOf("T"));
        }
        return LocalDate.parse(dateStr);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteChallenge(@PathVariable Long id) {
        return challengeService.deleteChallenge(id);
    }

    @GetMapping
    public ApiResponse<Page<Challenge>> getChallenges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return challengeService.getChallenges(pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<Challenge> getChallenge(@PathVariable Long id) {
        return challengeService.getChallenge(id);
    }

    @PostMapping("/{id}/join")
    public ApiResponse<ChallengeParticipant> joinChallenge(@PathVariable Long id, HttpServletRequest request) {
        return challengeService.joinChallenge(id, getCurrentUsername(request));
    }

    @PostMapping("/{id}/checkin")
    public ApiResponse<ChallengeCheckIn> checkIn(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Integer pagesRead = body.get("pagesRead") != null ? ((Number) body.get("pagesRead")).intValue() : null;
        String note = (String) body.get("note");
        return challengeService.checkIn(id, getCurrentUsername(request), pagesRead, note);
    }

    @GetMapping("/participants/{participantId}/checkins")
    public ApiResponse<Page<ChallengeCheckIn>> getCheckIns(
            @PathVariable Long participantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInDate").descending());
        return challengeService.getCheckIns(participantId, pageable);
    }

    @GetMapping("/my")
    public ApiResponse<Page<ChallengeParticipant>> getMyChallenges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());
        return challengeService.getMyChallenges(getCurrentUsername(request), pageable);
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<Page<ChallengeParticipant>> getChallengeParticipants(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());
        return challengeService.getChallengeParticipants(id, pageable);
    }
}