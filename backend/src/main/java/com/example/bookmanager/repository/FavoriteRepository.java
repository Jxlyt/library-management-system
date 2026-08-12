package com.example.bookmanager.repository;

import com.example.bookmanager.entity.Favorite;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    Optional<Favorite> findByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);
}