package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.PrivateMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface MessageService {
    ApiResponse<PrivateMessage> sendMessage(String senderUsername, String receiverUsername, String content);
    ApiResponse<Page<PrivateMessage>> getConversation(String username, String otherUsername, Pageable pageable);
    ApiResponse<Page<PrivateMessage>> getConversations(String username, Pageable pageable);
    ApiResponse<Map<String, Object>> getUnreadCount(String username);
    ApiResponse<Void> markAsRead(String username, String senderUsername);
}