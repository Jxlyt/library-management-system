package com.example.bookmanager.repository;

import com.example.bookmanager.entity.BlindBox;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlindBoxRepository extends JpaRepository<BlindBox, Long> {
    List<BlindBox> findByStatus(String status);
    Page<BlindBox> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);
    long countByStatus(String status);
}