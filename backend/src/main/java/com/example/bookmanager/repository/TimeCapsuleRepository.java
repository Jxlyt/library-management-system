package com.example.bookmanager.repository;

import com.example.bookmanager.entity.TimeCapsule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeCapsuleRepository extends JpaRepository<TimeCapsule, Long> {
    List<TimeCapsule> findByBookId(Long bookId);
    Page<TimeCapsule> findByBookId(Long bookId, Pageable pageable);
}