package com.ecommerce.platform.dto;

import com.ecommerce.platform.common.AuthRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 登录请求对象。 */
@Data
public class LoginRequest {

    @NotNull(message = "role must not be null")
    private AuthRole role;

    @NotBlank(message = "username must not be blank")
    private String username;

    @NotBlank(message = "password must not be blank")
    private String password;
}
