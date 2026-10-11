package com.ecommerce.platform.service;

import com.ecommerce.platform.common.AuthRole;
import com.ecommerce.platform.dto.LoginRequest;
import com.ecommerce.platform.dto.RegisterRequest;
import com.ecommerce.platform.entity.UserBuyer;
import com.ecommerce.platform.mapper.UserBuyerMapper;
import com.ecommerce.platform.mapper.UserSellerMapper;
import com.ecommerce.platform.security.JwtUtil;
import com.ecommerce.platform.security.TokenStore;
import com.ecommerce.platform.service.Impl.AuthServiceImpl;
import com.ecommerce.platform.vo.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserAdminService userAdminService;

    @Mock
    private UserBuyerService userBuyerService;

    @Mock
    private UserSellerService userSellerService;

    @Mock
    private UserBuyerMapper userBuyerMapper;

    @Mock
    private UserSellerMapper userSellerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenStore tokenStore;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginBuyerSuccess() {
        UserBuyer buyer = new UserBuyer();
        buyer.setId(1L);
        buyer.setUsername("buyer01");
        buyer.setPassword("encoded");
        buyer.setNickname("buyer one");
        when(userBuyerService.getByUsername("buyer01")).thenReturn(buyer);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");
        when(jwtUtil.getAccessExpireSeconds()).thenReturn(900L);
        when(jwtUtil.getRefreshExpireSeconds()).thenReturn(604800L);
        when(tokenStore.newFamilyId()).thenReturn("family-1");
        when(tokenStore.issueRefresh(anyString(), anyString(), any(), anyString(), anyString(),
                any(Duration.class))).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setRole(AuthRole.BUYER);
        request.setUsername("buyer01");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("buyer01", response.getUsername());
        assertEquals(AuthRole.BUYER, response.getRole());
        assertEquals("buyer one", response.getDisplayName());
    }

    @Test
    void loginWrongPasswordThrows() {
        UserBuyer buyer = new UserBuyer();
        buyer.setId(1L);
        buyer.setUsername("buyer01");
        buyer.setPassword("encoded");
        when(userBuyerService.getByUsername("buyer01")).thenReturn(buyer);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setRole(AuthRole.BUYER);
        request.setUsername("buyer01");
        request.setPassword("wrong");

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void registerDuplicateBuyerThrows() {
        RegisterRequest request = new RegisterRequest();
        request.setRole(AuthRole.BUYER);
        request.setUsername("buyer01");
        request.setPassword("123456");
        UserBuyer existing = new UserBuyer();
        existing.setId(1L);
        existing.setUsername("buyer01");
        existing.setDeleted(0);
        when(userBuyerMapper.selectAnyByUsername(anyString())).thenReturn(existing);

        assertThrows(RuntimeException.class, () -> authService.register(request));
    }
}
