package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.GroupPost;
import com.example.bookmanager.entity.ReadingGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReadingGroupService {
    ApiResponse<ReadingGroup> createGroup(String username, String name, String description, String coverColor);
    ApiResponse<Void> joinGroup(Long groupId, String username);
    ApiResponse<Void> leaveGroup(Long groupId, String username);
    ApiResponse<GroupPost> createPost(Long groupId, String username, String title, String content);
    ApiResponse<Void> deletePost(Long postId, String username);
    ApiResponse<Page<ReadingGroup>> getGroups(String keyword, Pageable pageable);
    ApiResponse<Page<ReadingGroup>> getMyGroups(String username, Pageable pageable);
    ApiResponse<Page<GroupPost>> getGroupPosts(Long groupId, Pageable pageable);
    ApiResponse<ReadingGroup> getGroup(Long groupId);
    ApiResponse<Boolean> checkMembership(Long groupId, String username);
}