package com.example.bookmanager.service;

import com.example.bookmanager.entity.User;
import com.example.bookmanager.dto.LoginRequest;
import com.example.bookmanager.dto.LoginResponse;

import java.util.Map;

public interface UserService {

    String register(LoginRequest request);

    LoginResponse login(LoginRequest request);

    User updateProfile(String username, Map<String, String> body);

    void changePassword(String username, String oldPassword, String newPassword);
}