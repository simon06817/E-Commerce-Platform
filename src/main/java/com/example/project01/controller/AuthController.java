package com.example.project01.controller;

import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.LoginRequest;
import com.example.project01.dto.RegisterRequest;
import com.example.project01.security.LoginRateLimiter;
import com.example.project01.service.AuthService;
import com.example.project01.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Authentication", description = "role-based login and registration")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    @Operation(summary = "Login", description = "login as ADMIN, BUYER or SELLER")
    @PostMapping("/login")
    public Result<LoginResponse> login(HttpServletRequest httpRequest,
                                       @RequestBody @Valid LoginRequest request) {
        String key = httpRequest.getRemoteAddr() + ":" + request.getUsername();
        if (!loginRateLimiter.allow(key)) {
            return Result.error(ResultCode.TOO_MANY_REQUESTS);
        }
        return Result.success(authService.login(request));
    }

    @Operation(summary = "Register", description = "register a buyer or seller account")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }
}
