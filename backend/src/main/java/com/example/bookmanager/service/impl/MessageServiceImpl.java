package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.PrivateMessage;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.PrivateMessageRepository;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    private final PrivateMessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageServiceImpl(PrivateMessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse<PrivateMessage> sendMessage(String senderUsername, String receiverUsername, String content) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("发送者不存在"));
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("接收者不存在"));

        PrivateMessage msg = new PrivateMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        PrivateMessage saved = messageRepository.save(msg);
        return ApiResponse.success("发送成功", saved);
    }

    @Override
    public ApiResponse<Page<PrivateMessage>> getConversation(String username, String otherUsername, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Page<PrivateMessage> messages = messageRepository.findConversation(user, other, pageable);
        return ApiResponse.success(messages);
    }

    @Override
    public ApiResponse<Page<PrivateMessage>> getConversations(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(messageRepository.findConversations(user, pageable));
    }

    @Override
    public ApiResponse<Map<String, Object>> getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("count", messageRepository.countUnread(user));
        return ApiResponse.success(result);
    }

    @Override
    @Transactional
    public ApiResponse<Void> markAsRead(String username, String senderUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        messageRepository.markAsRead(user, sender);
        return ApiResponse.success(null);
    }
}