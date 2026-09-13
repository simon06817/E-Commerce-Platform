package com.example.project01.service.Impl;

import com.example.project01.common.AuthRole;
import com.example.project01.common.BusinessException;
import com.example.project01.common.LoginUser;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.LoginRequest;
import com.example.project01.dto.RegisterRequest;
import com.example.project01.entity.UserAdmin;
import com.example.project01.entity.UserBuyer;
import com.example.project01.entity.UserSeller;
import com.example.project01.security.JwtUtil;
import com.example.project01.security.TokenStore;
import com.example.project01.service.AuthService;
import com.example.project01.service.UserAdminService;
import com.example.project01.service.UserBuyerService;
import com.example.project01.service.UserSellerService;
import com.example.project01.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Authenticates accounts against the role-specific tables, issues short-lived
 * access tokens plus refresh tokens and supports logout/refresh flows.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAdminService userAdminService;
    private final UserBuyerService userBuyerService;
    private final UserSellerService userSellerService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenStore tokenStore;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Resolve the correct role table before verifying the password.
        String username = request.getUsername().trim();
        String displayName = username;
        Long userId;
        String storedPassword;
        switch (request.getRole()) {
            case ADMIN -> {
                UserAdmin admin = userAdminService.getByUsername(username);
                if (admin == null) {
                    throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
                }
                userId = admin.getId();
                storedPassword = admin.getPassword();
            }
            case BUYER -> {
                UserBuyer buyer = userBuyerService.getByUsername(username);
                if (buyer == null) {
                    throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
                }
                userId = buyer.getId();
                storedPassword = buyer.getPassword();
                if (buyer.getNickname() != null && !buyer.getNickname().isBlank()) {
                    displayName = buyer.getNickname();
                }
            }
            case SELLER -> {
                UserSeller seller = userSellerService.getByUsername(username);
                if (seller == null) {
                    throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
                }
                userId = seller.getId();
                storedPassword = seller.getPassword();
                if (seller.getShopName() != null && !seller.getShopName().isBlank()) {
                    displayName = seller.getShopName();
                }
            }
            default -> throw new BusinessException(ResultCode.INVALID_ROLE);
        }

        if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        LoginUser loginUser = new LoginUser(userId, username, request.getRole());
        return issueTokens(loginUser, displayName);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (request.getRole() == AuthRole.ADMIN) {
            throw new BusinessException(ResultCode.INVALID_ROLE);
        }
        String encoded = passwordEncoder.encode(request.getPassword());
        if (request.getRole() == AuthRole.BUYER) {
            if (userBuyerService.existsByUsername(request.getUsername())) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
            }
            UserBuyer buyer = new UserBuyer();
            buyer.setUsername(request.getUsername());
            buyer.setPassword(encoded);
            buyer.setNickname(request.getNickname());
            buyer.setPhone(request.getPhone());
            buyer.setEmail(request.getEmail());
            buyer.setAddress(request.getAddress());
            userBuyerService.save(buyer);

            String displayName = buyer.getNickname() == null || buyer.getNickname().isBlank()
                    ? buyer.getUsername() : buyer.getNickname();
            return issueTokens(new LoginUser(buyer.getId(), buyer.getUsername(), AuthRole.BUYER), displayName);
        } else {
            if (userSellerService.existsByUsername(request.getUsername())) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
            }
            UserSeller seller = new UserSeller();
            seller.setUsername(request.getUsername());
            seller.setPassword(encoded);
            seller.setShopName(request.getShopName());
            seller.setPhone(request.getPhone());
            seller.setEmail(request.getEmail());
            userSellerService.save(seller);

            String displayName = seller.getShopName() == null || seller.getShopName().isBlank()
                    ? seller.getUsername() : seller.getShopName();
            return issueTokens(new LoginUser(seller.getId(), seller.getUsername(), AuthRole.SELLER), displayName);
        }
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        TokenStore.RefreshInfo info = tokenStore.getRefresh(refreshToken)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED));
        // Rotate the refresh token so the old one cannot be replayed.
        tokenStore.deleteRefresh(refreshToken);
        LoginUser user = new LoginUser(info.userId(), info.username(), AuthRole.valueOf(info.role()));
        return issueTokens(user, info.displayName());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenStore.deleteRefresh(refreshToken);
        }
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            try {
                String raw = accessToken.substring(7);
                LoginUser user = jwtUtil.parseToken(raw);
                tokenStore.blacklistAccess(user.getJti(), Duration.ofMillis(jwtUtil.getRemainingMillis(raw)));
            } catch (Exception ignored) {
                // Logout still succeeds even when the access token already expired.
            }
        }
    }

    private LoginResponse issueTokens(LoginUser user, String displayName) {
        String refreshId = UUID.randomUUID().toString();
        tokenStore.saveRefresh(
                refreshId,
                user.getRole().name(),
                user.getId(),
                user.getUsername(),
                displayName,
                Duration.ofSeconds(jwtUtil.getRefreshExpireSeconds()));
        return new LoginResponse(
                jwtUtil.generateToken(user),
                refreshId,
                user.getId(),
                user.getUsername(),
                user.getRole(),
                displayName,
                jwtUtil.getAccessExpireSeconds(),
                jwtUtil.getRefreshExpireSeconds());
    }
}
