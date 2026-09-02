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
import com.example.project01.service.AuthService;
import com.example.project01.service.UserAdminService;
import com.example.project01.service.UserBuyerService;
import com.example.project01.service.UserSellerService;
import com.example.project01.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAdminService userAdminService;
    private final UserBuyerService userBuyerService;
    private final UserSellerService userSellerService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
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
        return new LoginResponse(jwtUtil.generateToken(loginUser), userId, username, request.getRole(), displayName);
    }

    @Override
    public void register(RegisterRequest request) {
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
        }
    }
}
