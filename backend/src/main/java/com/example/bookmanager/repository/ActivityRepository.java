package com.example.bookmanager.repository;

import com.example.bookmanager.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Activity> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);
}