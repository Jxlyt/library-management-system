package com.example.bookmanager.repository;

import com.example.bookmanager.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    long countByFollowerId(Long userId);
    long countByFolloweeId(Long userId);
    Page<UserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);
    Page<UserFollow> findByFolloweeIdOrderByCreatedAtDesc(Long followeeId, Pageable pageable);
}