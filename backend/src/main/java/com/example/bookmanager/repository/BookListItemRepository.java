package com.example.bookmanager.repository;

import com.example.bookmanager.entity.BookListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookListItemRepository extends JpaRepository<BookListItem, Long> {
    List<BookListItem> findByBookListIdOrderBySortOrderAsc(Long bookListId);
    void deleteByBookListId(Long bookListId);
}