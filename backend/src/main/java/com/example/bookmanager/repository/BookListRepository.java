package com.example.bookmanager.repository;

import com.example.bookmanager.entity.BookList;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookListRepository extends JpaRepository<BookList, Long> {
    Page<BookList> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);
    Page<BookList> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}