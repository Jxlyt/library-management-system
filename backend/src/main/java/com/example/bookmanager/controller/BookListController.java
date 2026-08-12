package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.BookList;
import com.example.bookmanager.entity.BookListItem;
import com.example.bookmanager.service.BookListService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/booklists")
public class BookListController {

    private final BookListService bookListService;

    public BookListController(BookListService bookListService) {
        this.bookListService = bookListService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping
    public ApiResponse<BookList> createBookList(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Boolean isPublic = body.get("isPublic") != null ? (Boolean) body.get("isPublic") : true;
        String coverUrl = (String) body.get("coverUrl");
        return bookListService.createBookList(getCurrentUsername(request), name, description, isPublic, coverUrl);
    }

    @PutMapping("/{listId}")
    public ApiResponse<BookList> updateBookList(@PathVariable Long listId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Boolean isPublic = body.get("isPublic") != null ? (Boolean) body.get("isPublic") : null;
        String coverUrl = (String) body.get("coverUrl");
        return bookListService.updateBookList(listId, getCurrentUsername(request), name, description, isPublic, coverUrl);
    }

    @DeleteMapping("/{listId}")
    public ApiResponse<?> deleteBookList(@PathVariable Long listId, HttpServletRequest request) {
        return bookListService.deleteBookList(listId, getCurrentUsername(request));
    }

    @GetMapping("/{listId}")
    public ApiResponse<BookList> getBookList(@PathVariable Long listId) {
        return bookListService.getBookList(listId);
    }

    @GetMapping("/public")
    public ApiResponse<Page<BookList>> getPublicBookLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return bookListService.getPublicBookLists(pageable);
    }

    @GetMapping("/my")
    public ApiResponse<Page<BookList>> getMyBookLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return bookListService.getMyBookLists(getCurrentUsername(request), pageable);
    }

    @PostMapping("/{listId}/items")
    public ApiResponse<BookListItem> addBookToList(@PathVariable Long listId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long bookId = ((Number) body.get("bookId")).longValue();
        return bookListService.addBookToList(listId, getCurrentUsername(request), bookId);
    }

    @DeleteMapping("/{listId}/items/{bookId}")
    public ApiResponse<?> removeBookFromList(@PathVariable Long listId, @PathVariable Long bookId, HttpServletRequest request) {
        return bookListService.removeBookFromList(listId, getCurrentUsername(request), bookId);
    }

    @GetMapping("/{listId}/items")
    public ApiResponse<List<BookListItem>> getBookListItems(@PathVariable Long listId) {
        return bookListService.getBookListItems(listId);
    }

    @PostMapping("/{listId}/favorite")
    public ApiResponse<?> favoriteBookList(@PathVariable Long listId, HttpServletRequest request) {
        return bookListService.favoriteBookList(listId, getCurrentUsername(request));
    }

    @DeleteMapping("/{listId}/favorite")
    public ApiResponse<?> unfavoriteBookList(@PathVariable Long listId, HttpServletRequest request) {
        return bookListService.unfavoriteBookList(listId, getCurrentUsername(request));
    }
}