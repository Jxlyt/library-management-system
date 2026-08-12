package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.BorrowRecord;
import com.example.bookmanager.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface BorrowService {

    ApiResponse<BorrowRecord> borrowBook(Long bookId, String username, LocalDateTime borrowDate, LocalDateTime dueDate);

    ApiResponse<BorrowRecord> returnBook(Long borrowId, String username);

    ApiResponse<BorrowRecord> payFine(Long borrowId, String username);

    ApiResponse<BorrowRecord> renewBook(Long borrowId, String username);

    ApiResponse<Reservation> reserveBook(Long bookId, String username);

    ApiResponse<Void> cancelReservation(Long reservationId, String username);

    ApiResponse<Page<BorrowRecord>> getMyBorrowHistory(String username, Pageable pageable);

    ApiResponse<Page<BorrowRecord>> getAllBorrows(String keyword, Pageable pageable);

    ApiResponse<Page<Reservation>> getMyReservations(String username, Pageable pageable);

    ApiResponse<Page<Reservation>> getAllReservations(Pageable pageable);

    ApiResponse<LocalDateTime> getEarliestReturnDate(Long bookId);
}