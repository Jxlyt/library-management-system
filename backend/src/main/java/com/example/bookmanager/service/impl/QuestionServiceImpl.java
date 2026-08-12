package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public QuestionServiceImpl(QuestionRepository questionRepository,
                               AnswerRepository answerRepository,
                               UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse<Question> createQuestion(String username, String title, String content) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error(400, "标题不能为空");
        }
        Question question = new Question();
        question.setUser(user);
        question.setTitle(title);
        question.setContent(content != null ? content : "");
        return ApiResponse.success("发布成功", questionRepository.save(question));
    }

    @Override
    public ApiResponse<Question> getQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问题不存在"));
        question.setViewCount(question.getViewCount() + 1);
        questionRepository.save(question);
        return ApiResponse.success(question);
    }

    @Override
    public ApiResponse<Page<Question>> getQuestions(Pageable pageable) {
        return ApiResponse.success(questionRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteQuestion(Long questionId, String username) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问题不存在"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!question.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            return ApiResponse.error(403, "只能删除自己的问题");
        }
        questionRepository.delete(question);
        return ApiResponse.success("删除成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<Answer> createAnswer(Long questionId, String username, String content) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问题不存在"));
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error(400, "回答内容不能为空");
        }
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setUser(user);
        answer.setContent(content);
        Answer saved = answerRepository.save(answer);
        question.setAnswerCount(question.getAnswerCount() + 1);
        questionRepository.save(question);
        return ApiResponse.success("回答成功", saved);
    }

    @Override
    public ApiResponse<Page<Answer>> getAnswers(Long questionId, Pageable pageable) {
        return ApiResponse.success(answerRepository.findByQuestionIdOrderByCreatedAtAsc(questionId, pageable));
    }

    @Override
    @Transactional
    public ApiResponse<?> acceptAnswer(Long questionId, Long answerId, String username) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("问题不存在"));
        if (!question.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能采纳自己问题的答案");
        }
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("回答不存在"));
        answer.setIsAccepted(true);
        answerRepository.save(answer);
        question.setBestAnswerId(answerId);
        questionRepository.save(question);
        return ApiResponse.success("已采纳最佳答案", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> likeAnswer(Long answerId, String username) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("回答不存在"));
        answer.setLikeCount(answer.getLikeCount() + 1);
        answerRepository.save(answer);
        return ApiResponse.success("点赞成功", null);
    }
}