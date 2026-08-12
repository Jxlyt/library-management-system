package com.example.bookmanager.repository;

import com.example.bookmanager.entity.BookQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookQuoteRepository extends JpaRepository<BookQuote, Long> {
    Page<BookQuote> findAllByOrderByCreatedAtDesc(Pageable pageable);
}