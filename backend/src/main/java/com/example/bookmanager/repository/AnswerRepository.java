package com.example.bookmanager.repository;

import com.example.bookmanager.entity.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Page<Answer> findByQuestionIdOrderByCreatedAtAsc(Long questionId, Pageable pageable);
}