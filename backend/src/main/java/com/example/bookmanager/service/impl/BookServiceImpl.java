package com.example.bookmanager.service.impl;

import com.example.bookmanager.entity.Book;
import com.example.bookmanager.repository.BookRepository;
import com.example.bookmanager.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Page<Book> findAll(String keyword, String category, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return bookRepository.searchByKeyword(keyword.trim(), pageable);
        }
        if (StringUtils.hasText(category)) {
            return bookRepository.findByCategory(category.trim(), pageable);
        }
        return bookRepository.findAll(pageable);
    }

    @Override
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public Book update(Long id, Book book) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("图书不存在: " + id));
        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());
        existing.setIsbn(book.getIsbn());
        existing.setCategory(book.getCategory());
        existing.setPublisher(book.getPublisher());
        existing.setPageCount(book.getPageCount());
        existing.setDescription(book.getDescription());
        existing.setImageUrl(book.getImageUrl());
        existing.setPublicationDate(book.getPublicationDate());
        if (book.getTotalCopies() != null) {
            existing.setTotalCopies(book.getTotalCopies());
        }
        if (book.getAvailableCopies() != null) {
            existing.setAvailableCopies(book.getAvailableCopies());
        }
        existing.setSalePrice(book.getSalePrice());
        existing.setDiscount(book.getDiscount());
        if (book.getSaleableCopies() != null) {
            existing.setSaleableCopies(book.getSaleableCopies());
        }
        return bookRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }
}