package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import java.util.Map;

public interface PointService {
    void addPoints(String username, int points, String reason);
    int calculateLevel(int points);
    String getLevelName(int level);
    ApiResponse<Map<String, Object>> getMyLevel(String username);
}