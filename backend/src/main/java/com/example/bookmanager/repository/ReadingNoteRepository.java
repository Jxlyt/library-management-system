package com.example.bookmanager.repository;

import com.example.bookmanager.entity.ReadingNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingNoteRepository extends JpaRepository<ReadingNote, Long> {

    Page<ReadingNote> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ReadingNote> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    Page<ReadingNote> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<ReadingNote> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}