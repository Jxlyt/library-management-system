package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.BookListService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookListServiceImpl implements BookListService {

    private final BookListRepository bookListRepository;
    private final BookListItemRepository bookListItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookListServiceImpl(BookListRepository bookListRepository,
                               BookListItemRepository bookListItemRepository,
                               BookRepository bookRepository,
                               UserRepository userRepository) {
        this.bookListRepository = bookListRepository;
        this.bookListItemRepository = bookListItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse<BookList> createBookList(String username, String name, String description, Boolean isPublic, String coverUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (name == null || name.trim().isEmpty()) {
            return ApiResponse.error(400, "书单名称不能为空");
        }
        BookList bookList = new BookList();
        bookList.setUser(user);
        bookList.setName(name);
        bookList.setDescription(description);
        bookList.setIsPublic(isPublic != null ? isPublic : true);
        bookList.setCoverUrl(coverUrl);
        return ApiResponse.success("创建成功", bookListRepository.save(bookList));
    }

    @Override
    @Transactional
    public ApiResponse<BookList> updateBookList(Long listId, String username, String name, String description, Boolean isPublic, String coverUrl) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        if (!bookList.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能修改自己的书单");
        }
        if (name != null && !name.trim().isEmpty()) bookList.setName(name);
        if (description != null) bookList.setDescription(description);
        if (isPublic != null) bookList.setIsPublic(isPublic);
        if (coverUrl != null) bookList.setCoverUrl(coverUrl);
        return ApiResponse.success("更新成功", bookListRepository.save(bookList));
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteBookList(Long listId, String username) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        if (!bookList.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能删除自己的书单");
        }
        bookListItemRepository.deleteByBookListId(listId);
        bookListRepository.delete(bookList);
        return ApiResponse.success("删除成功", null);
    }

    @Override
    public ApiResponse<BookList> getBookList(Long listId) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        return ApiResponse.success(bookList);
    }

    @Override
    public ApiResponse<Page<BookList>> getPublicBookLists(Pageable pageable) {
        return ApiResponse.success(bookListRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable));
    }

    @Override
    public ApiResponse<Page<BookList>> getMyBookLists(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(bookListRepository.findByUserOrderByCreatedAtDesc(user, pageable));
    }

    @Override
    @Transactional
    public ApiResponse<BookListItem> addBookToList(Long listId, String username, Long bookId) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        if (!bookList.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能修改自己的书单");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));
        BookListItem item = new BookListItem();
        item.setBookList(bookList);
        item.setBook(book);
        item.setSortOrder(bookListItemRepository.findByBookListIdOrderBySortOrderAsc(listId).size());
        return ApiResponse.success("添加成功", bookListItemRepository.save(item));
    }

    @Override
    @Transactional
    public ApiResponse<?> removeBookFromList(Long listId, String username, Long bookId) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        if (!bookList.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "只能修改自己的书单");
        }
        List<BookListItem> items = bookListItemRepository.findByBookListIdOrderBySortOrderAsc(listId);
        items.stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst()
                .ifPresent(bookListItemRepository::delete);
        return ApiResponse.success("移除成功", null);
    }

    @Override
    public ApiResponse<List<BookListItem>> getBookListItems(Long listId) {
        return ApiResponse.success(bookListItemRepository.findByBookListIdOrderBySortOrderAsc(listId));
    }

    @Override
    @Transactional
    public ApiResponse<?> favoriteBookList(Long listId, String username) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        bookList.setFavoriteCount(bookList.getFavoriteCount() + 1);
        bookListRepository.save(bookList);
        return ApiResponse.success("收藏成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<?> unfavoriteBookList(Long listId, String username) {
        BookList bookList = bookListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("书单不存在"));
        if (bookList.getFavoriteCount() > 0) {
            bookList.setFavoriteCount(bookList.getFavoriteCount() - 1);
            bookListRepository.save(bookList);
        }
        return ApiResponse.success("已取消收藏", null);
    }
}