package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.PointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;

    public PointServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void addPoints(String username, int points, String reason) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;
        int newPoints = user.getPoints() + points;
        user.setPoints(newPoints);
        user.setLevel(calculateLevel(newPoints));
        userRepository.save(user);
    }

    @Override
    public int calculateLevel(int points) {
        if (points >= 1000) return 4; // 书圣
        if (points >= 300) return 3;  // 书痴
        if (points >= 100) return 2;  // 书迷
        return 1;                     // 书虫
    }

    @Override
    public String getLevelName(int level) {
        switch (level) {
            case 4: return "书圣";
            case 3: return "书痴";
            case 2: return "书迷";
            default: return "书虫";
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getMyLevel(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        int nextLevelPoints;
        switch (user.getLevel()) {
            case 1: nextLevelPoints = 100; break;
            case 2: nextLevelPoints = 300; break;
            case 3: nextLevelPoints = 1000; break;
            default: nextLevelPoints = -1;
        }
        Map<String, Object> result = Map.of(
                "points", user.getPoints(),
                "level", user.getLevel(),
                "levelName", getLevelName(user.getLevel()),
                "nextLevelPoints", nextLevelPoints
        );
        return ApiResponse.success(result);
    }
}