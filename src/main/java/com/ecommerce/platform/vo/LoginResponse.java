package com.ecommerce.platform.vo;

import com.ecommerce.platform.common.AuthRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Login result containing the JWT and basic account information.
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    @JsonIgnore
    private String refreshToken;

    private Long userId;

    private String username;

    private AuthRole role;

    private String displayName;

    private Long accessExpiresIn;

    @JsonIgnore
    private Long refreshExpiresIn;
}
