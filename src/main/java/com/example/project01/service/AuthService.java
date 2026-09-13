package com.example.project01.service;

import com.example.project01.dto.LoginRequest;
import com.example.project01.dto.RegisterRequest;
import com.example.project01.vo.LoginResponse;

/**
 * Login and registration use cases for all three roles.
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    LoginResponse refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);
}
