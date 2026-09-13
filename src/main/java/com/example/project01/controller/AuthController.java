package com.example.project01.controller;

import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.common.LoginUser;
import com.example.project01.dto.LoginRequest;
import com.example.project01.dto.RefreshTokenRequest;
import com.example.project01.dto.RegisterRequest;
import com.example.project01.security.LoginRateLimiter;
import com.example.project01.service.AuthService;
import com.example.project01.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authentication entry points for role selection, login and registration.
 */
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
    public Result<LoginResponse> register(@RequestBody @Valid RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "Current user", description = "return the authenticated user from the JWT")
    @GetMapping("/me")
    public Result<LoginUser> me(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(loginUser);
    }

    @Operation(summary = "Refresh token", description = "exchange a refresh token for a new access token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return Result.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "Logout", description = "blacklist the access token and delete the refresh token")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestBody(required = false) RefreshTokenRequest request) {
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.logout(authorization, refreshToken);
        return Result.success();
    }
}
