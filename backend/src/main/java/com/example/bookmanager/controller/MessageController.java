package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.PrivateMessage;
import com.example.bookmanager.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping("/send")
    public ApiResponse<PrivateMessage> sendMessage(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String receiver = body.get("receiver");
        String content = body.get("content");
        return messageService.sendMessage(getCurrentUsername(request), receiver, content);
    }

    @GetMapping("/conversation/{otherUsername}")
    public ApiResponse<Page<PrivateMessage>> getConversation(
            @PathVariable String otherUsername,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return messageService.getConversation(getCurrentUsername(request), otherUsername, pageable);
    }

    @GetMapping("/conversations")
    public ApiResponse<Page<PrivateMessage>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return messageService.getConversations(getCurrentUsername(request), pageable);
    }

    @GetMapping("/unread")
    public ApiResponse<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        return messageService.getUnreadCount(getCurrentUsername(request));
    }

    @PostMapping("/read/{senderUsername}")
    public ApiResponse<Void> markAsRead(@PathVariable String senderUsername, HttpServletRequest request) {
        return messageService.markAsRead(getCurrentUsername(request), senderUsername);
    }
}