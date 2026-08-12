package com.example.bookmanager.repository;

import com.example.bookmanager.entity.ReadingGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingGroupRepository extends JpaRepository<ReadingGroup, Long> {
    Page<ReadingGroup> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ReadingGroup> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}