package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.GroupPost;
import com.example.bookmanager.entity.ReadingGroup;
import com.example.bookmanager.service.ReadingGroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class ReadingGroupController {

    private final ReadingGroupService groupService;

    public ReadingGroupController(ReadingGroupService groupService) {
        this.groupService = groupService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping
    public ApiResponse<ReadingGroup> createGroup(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return groupService.createGroup(
                getCurrentUsername(request),
                body.get("name"),
                body.get("description"),
                body.get("coverColor")
        );
    }

    @GetMapping
    public ApiResponse<Page<ReadingGroup>> getGroups(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return groupService.getGroups(keyword, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReadingGroup> getGroup(@PathVariable Long id) {
        return groupService.getGroup(id);
    }

    @GetMapping("/my")
    public ApiResponse<Page<ReadingGroup>> getMyGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());
        return groupService.getMyGroups(getCurrentUsername(request), pageable);
    }

    @PostMapping("/{id}/join")
    public ApiResponse<Void> joinGroup(@PathVariable Long id, HttpServletRequest request) {
        return groupService.joinGroup(id, getCurrentUsername(request));
    }

    @PostMapping("/{id}/leave")
    public ApiResponse<Void> leaveGroup(@PathVariable Long id, HttpServletRequest request) {
        return groupService.leaveGroup(id, getCurrentUsername(request));
    }

    @GetMapping("/{id}/check")
    public ApiResponse<Boolean> checkMembership(@PathVariable Long id, HttpServletRequest request) {
        return groupService.checkMembership(id, getCurrentUsername(request));
    }

    @GetMapping("/{id}/posts")
    public ApiResponse<Page<GroupPost>> getGroupPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return groupService.getGroupPosts(id, pageable);
    }

    @PostMapping("/{id}/posts")
    public ApiResponse<GroupPost> createPost(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        return groupService.createPost(id, getCurrentUsername(request), body.get("title"), body.get("content"));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId, HttpServletRequest request) {
        return groupService.deletePost(postId, getCurrentUsername(request));
    }
}