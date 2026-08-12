package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Answer;
import com.example.bookmanager.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionService {
    ApiResponse<Question> createQuestion(String username, String title, String content);
    ApiResponse<Question> getQuestion(Long questionId);
    ApiResponse<Page<Question>> getQuestions(Pageable pageable);
    ApiResponse<?> deleteQuestion(Long questionId, String username);
    ApiResponse<Answer> createAnswer(Long questionId, String username, String content);
    ApiResponse<Page<Answer>> getAnswers(Long questionId, Pageable pageable);
    ApiResponse<?> acceptAnswer(Long questionId, Long answerId, String username);
    ApiResponse<?> likeAnswer(Long answerId, String username);
}