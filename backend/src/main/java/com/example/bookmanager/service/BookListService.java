package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.BookList;
import com.example.bookmanager.entity.BookListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookListService {
    ApiResponse<BookList> createBookList(String username, String name, String description, Boolean isPublic, String coverUrl);
    ApiResponse<BookList> updateBookList(Long listId, String username, String name, String description, Boolean isPublic, String coverUrl);
    ApiResponse<?> deleteBookList(Long listId, String username);
    ApiResponse<BookList> getBookList(Long listId);
    ApiResponse<Page<BookList>> getPublicBookLists(Pageable pageable);
    ApiResponse<Page<BookList>> getMyBookLists(String username, Pageable pageable);
    ApiResponse<BookListItem> addBookToList(Long listId, String username, Long bookId);
    ApiResponse<?> removeBookFromList(Long listId, String username, Long bookId);
    ApiResponse<List<BookListItem>> getBookListItems(Long listId);
    ApiResponse<?> favoriteBookList(Long listId, String username);
    ApiResponse<?> unfavoriteBookList(Long listId, String username);
}