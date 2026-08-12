package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.BlindBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlindBoxService {
    ApiResponse<BlindBox> createBlindBox(Long bookId, Double price, String category);
    ApiResponse<BlindBox> openBlindBox(String username, String category);
    ApiResponse<Page<BlindBox>> getMyBlindBoxes(String username, Pageable pageable);
    ApiResponse<?> getActiveCount();
}