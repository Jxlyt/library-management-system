package com.example.bookmanager.repository;

import com.example.bookmanager.entity.ReadingPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingPartnerRepository extends JpaRepository<ReadingPartner, Long> {
    Optional<ReadingPartner> findByUserIdAndMatchDate(Long userId, String matchDate);
    void deleteByMatchDate(String matchDate);
}