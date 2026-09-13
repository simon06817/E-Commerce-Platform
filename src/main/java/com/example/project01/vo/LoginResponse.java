package com.example.project01.vo;

import com.example.project01.common.AuthRole;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Login result containing the JWT and basic account information.
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String refreshToken;

    private Long userId;

    private String username;

    private AuthRole role;

    private String displayName;

    private Long accessExpiresIn;

    private Long refreshExpiresIn;
}
