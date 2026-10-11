package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.LoginRequest;
import com.ecommerce.platform.dto.RegisterRequest;
import com.ecommerce.platform.vo.LoginResponse;

/**
 * Login and registration use cases for all three roles.
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    LoginResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
