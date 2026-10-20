package com.ecommerce.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 刷新令牌请求对象。 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refresh token must not be blank")
    private String refreshToken;
}
