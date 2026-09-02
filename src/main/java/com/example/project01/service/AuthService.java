package com.example.project01.service;

import com.example.project01.dto.LoginRequest;
import com.example.project01.dto.RegisterRequest;
import com.example.project01.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);
}
