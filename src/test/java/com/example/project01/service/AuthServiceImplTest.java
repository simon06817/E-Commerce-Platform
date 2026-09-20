package com.example.project01.service;

import com.example.project01.common.AuthRole;
import com.example.project01.dto.LoginRequest;
import com.example.project01.dto.RegisterRequest;
import com.example.project01.entity.UserBuyer;
import com.example.project01.mapper.UserBuyerMapper;
import com.example.project01.mapper.UserSellerMapper;
import com.example.project01.security.JwtUtil;
import com.example.project01.security.TokenStore;
import com.example.project01.service.Impl.AuthServiceImpl;
import com.example.project01.vo.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

        LoginRequest request = new LoginRequest();
        request.setRole(AuthRole.BUYER);
        request.setUsername("buyer01");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
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
