package com.ecommerce.platform.vo;

import com.ecommerce.platform.common.AuthRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录返回对象。 */
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
