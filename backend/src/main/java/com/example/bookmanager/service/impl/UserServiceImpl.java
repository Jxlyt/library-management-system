package com.example.bookmanager.service.impl;

import com.example.bookmanager.entity.User;
import com.example.bookmanager.dto.LoginRequest;
import com.example.bookmanager.dto.LoginResponse;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.UserService;
import com.example.bookmanager.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public String register(LoginRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ?
                User.Role.valueOf(request.getRole().toUpperCase()) :
                User.Role.USER);

        userRepository.save(user);
        return "注册成功";
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (!user.getRole().name().equalsIgnoreCase(request.getRole())) {
            throw new RuntimeException("角色不匹配");
        }

        if ("DISABLED".equals(user.getStatus())) {
            throw new RuntimeException("该账户已被禁用，请联系管理员");
        }
        if ("LOST".equals(user.getStatus())) {
            throw new RuntimeException("该账户已挂失，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    @Override
    public User updateProfile(String username, Map<String, String> body) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (body.containsKey("nickname")) {
            user.setNickname(body.get("nickname"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar(body.get("avatar"));
        }
        if (body.containsKey("phone")) {
            user.setPhone(body.get("phone"));
        }
        if (body.containsKey("email")) {
            user.setEmail(body.get("email"));
        }

        return userRepository.save(user);
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}