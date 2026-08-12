package com.example.bookmanager.service;

import com.example.bookmanager.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookService {

    Page<Book> findAll(String keyword, String category, Pageable pageable);

    Optional<Book> findById(Long id);

    Book save(Book book);

    Book update(Long id, Book book);

    void deleteById(Long id);
}