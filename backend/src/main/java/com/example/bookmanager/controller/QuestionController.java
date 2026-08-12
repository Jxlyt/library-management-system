package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Answer;
import com.example.bookmanager.entity.Question;
import com.example.bookmanager.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qa")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping("/questions")
    public ApiResponse<Question> createQuestion(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        return questionService.createQuestion(getCurrentUsername(request), title, content);
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<Question> getQuestion(@PathVariable Long id) {
        return questionService.getQuestion(id);
    }

    @GetMapping("/questions")
    public ApiResponse<Page<Question>> getQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return questionService.getQuestions(pageable);
    }

    @DeleteMapping("/questions/{id}")
    public ApiResponse<?> deleteQuestion(@PathVariable Long id, HttpServletRequest request) {
        return questionService.deleteQuestion(id, getCurrentUsername(request));
    }

    @PostMapping("/questions/{questionId}/answers")
    public ApiResponse<Answer> createAnswer(@PathVariable Long questionId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String content = (String) body.get("content");
        return questionService.createAnswer(questionId, getCurrentUsername(request), content);
    }

    @GetMapping("/questions/{questionId}/answers")
    public ApiResponse<Page<Answer>> getAnswers(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return questionService.getAnswers(questionId, pageable);
    }

    @PostMapping("/answers/{answerId}/accept")
    public ApiResponse<?> acceptAnswer(@PathVariable Long answerId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long questionId = ((Number) body.get("questionId")).longValue();
        return questionService.acceptAnswer(questionId, answerId, getCurrentUsername(request));
    }

    @PostMapping("/answers/{answerId}/like")
    public ApiResponse<?> likeAnswer(@PathVariable Long answerId, HttpServletRequest request) {
        return questionService.likeAnswer(answerId, getCurrentUsername(request));
    }
}