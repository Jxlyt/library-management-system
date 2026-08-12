package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    /**
     * 分页查询用户列表，支持关键词搜索
     */
    @GetMapping
    public ApiResponse<Page<User>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> users;
        if (keyword != null && !keyword.isEmpty()) {
            users = userRepository.findByUsernameContaining(keyword, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return ApiResponse.success(users);
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/{id}")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (body.containsKey("phone")) {
            user.setPhone(body.get("phone"));
        }
        if (body.containsKey("email")) {
            user.setEmail(body.get("email"));
        }
        userRepository.save(user);
        return ApiResponse.success("修改成功", user);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ApiResponse<User> getProfile(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(user);
    }

    /**
     * 更新当前用户个人信息（昵称/头像/手机号/邮箱）
     */
    @PutMapping("/profile")
    public ApiResponse<User> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request) {
        User updated = userService.updateProfile(getCurrentUsername(request), body);
        return ApiResponse.success("个人信息更新成功", updated);
    }

    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.changePassword(getCurrentUsername(request), oldPassword, newPassword);
        return ApiResponse.success("密码修改成功，请重新登录", null);
    }

    /**
     * 启用账户
     */
    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("ACTIVE");
        userRepository.save(user);
        return ApiResponse.success("账户已启用", null);
    }

    /**
     * 禁用账户
     */
    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("DISABLED");
        userRepository.save(user);
        return ApiResponse.success("账户已禁用", null);
    }

    /**
     * 挂失
     */
    @PutMapping("/{id}/lost")
    public ApiResponse<Void> lost(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("LOST");
        userRepository.save(user);
        return ApiResponse.success("已挂失", null);
    }

    /**
     * 注销用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ApiResponse.success("注销成功", null);
    }
}