package com.ecommerce.platform.controller;

import com.ecommerce.platform.common.Result;
import com.ecommerce.platform.common.ResultCode;
import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.dto.LoginRequest;
import com.ecommerce.platform.dto.RegisterRequest;
import com.ecommerce.platform.security.LoginRateLimiter;
import com.ecommerce.platform.service.AuthService;
import com.ecommerce.platform.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

/**
 * Authentication entry points for role selection, login and registration.
 */
@Tag(name = "Authentication", description = "role-based login and registration")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    @Value("${app.auth.cookie-secure:false}")
    private boolean secureCookie;

    @Operation(summary = "Login", description = "login as ADMIN, BUYER or SELLER")
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(HttpServletRequest httpRequest,
                                                       @RequestBody @Valid LoginRequest request) {
        String key = httpRequest.getRemoteAddr() + ":" + request.getUsername();
        if (!loginRateLimiter.allow(key)) {
            return ResponseEntity.ok(Result.error(ResultCode.TOO_MANY_REQUESTS));
        }
        return withRefreshCookie(authService.login(request));
    }

    @Operation(summary = "Register", description = "register a buyer or seller account")
    @PostMapping("/register")
    public ResponseEntity<Result<LoginResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return withRefreshCookie(authService.register(request));
    }

    @Operation(summary = "Current user", description = "return the authenticated user from the JWT")
    @GetMapping("/me")
    public Result<LoginUser> me(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(loginUser);
    }

    @Operation(summary = "Refresh token", description = "exchange a refresh token for a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<Result<LoginResponse>> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    @Operation(summary = "Logout", description = "revoke the refresh token and clear its cookie")
    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .body(Result.success());
    }

    private ResponseEntity<Result<LoginResponse>> withRefreshCookie(LoginResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(response.getRefreshToken(), response.getRefreshExpiresIn()).toString())
                .body(Result.success(response));
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value == null ? "" : value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .build();
    }
}
