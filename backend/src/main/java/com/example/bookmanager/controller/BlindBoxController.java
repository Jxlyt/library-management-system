package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.BlindBox;
import com.example.bookmanager.service.BlindBoxService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/blind-box")
public class BlindBoxController {

    private final BlindBoxService blindBoxService;

    public BlindBoxController(BlindBoxService blindBoxService) {
        this.blindBoxService = blindBoxService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping("/create")
    public ApiResponse<BlindBox> create(@RequestBody Map<String, Object> body) {
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Double price = body.get("price") != null ? Double.valueOf(body.get("price").toString()) : null;
        String category = body.get("category") != null ? body.get("category").toString() : null;
        return blindBoxService.createBlindBox(bookId, price, category);
    }

    @PostMapping("/open")
    public ApiResponse<BlindBox> open(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        String category = body != null ? body.get("category") : null;
        return blindBoxService.openBlindBox(getCurrentUsername(request), category);
    }

    @GetMapping("/my")
    public ApiResponse<Page<BlindBox>> getMyBlindBoxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blindBoxService.getMyBlindBoxes(getCurrentUsername(request), pageable);
    }

    @GetMapping("/count")
    public ApiResponse<?> getActiveCount() {
        return blindBoxService.getActiveCount();
    }
}