package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Announcement;
import com.example.bookmanager.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    private String getRole(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }

    private void checkAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(getRole(request))) {
            throw new RuntimeException("仅管理员可执行此操作");
        }
    }

    @GetMapping
    public ApiResponse<Page<Announcement>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(announcementService.findAll(pageable));
    }

    @GetMapping("/latest")
    public ApiResponse<List<Announcement>> getLatest() {
        return ApiResponse.success(announcementService.getLatest(5));
    }

    @PostMapping
    public ApiResponse<Announcement> create(@RequestBody Announcement announcement, HttpServletRequest request) {
        checkAdmin(request);
        return ApiResponse.success("公告发布成功", announcementService.create(announcement));
    }

    @PutMapping("/{id}")
    public ApiResponse<Announcement> update(@PathVariable Long id, @RequestBody Announcement announcement, HttpServletRequest request) {
        checkAdmin(request);
        return ApiResponse.success("公告更新成功", announcementService.update(id, announcement));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        announcementService.delete(id);
        return ApiResponse.success("公告已删除", null);
    }
}