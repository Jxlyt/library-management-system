package com.example.bookmanager.repository;

import com.example.bookmanager.entity.PrivateMessage;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    // 获取与某个用户的对话（发送或接收的）
    @Query("SELECT m FROM PrivateMessage m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.createdAt DESC")
    Page<PrivateMessage> findConversation(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

    // 获取用户的所有消息（发送或接收的），按时间倒序
    @Query("SELECT m FROM PrivateMessage m WHERE m.sender = :user OR m.receiver = :user ORDER BY m.createdAt DESC")
    Page<PrivateMessage> findConversations(@Param("user") User user, Pageable pageable);

    // 未读消息数量
    @Query("SELECT COUNT(m) FROM PrivateMessage m WHERE m.receiver = :user AND m.isRead = false")
    long countUnread(@Param("user") User user);

    // 标记消息为已读
    @Modifying
    @Transactional
    @Query("UPDATE PrivateMessage m SET m.isRead = true WHERE m.receiver = :user AND m.sender = :sender AND m.isRead = false")
    void markAsRead(@Param("user") User user, @Param("sender") User sender);
}