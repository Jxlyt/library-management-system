package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.service.PointService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("/level")
    public ApiResponse<Map<String, Object>> getMyLevel(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return pointService.getMyLevel(username);
    }
}