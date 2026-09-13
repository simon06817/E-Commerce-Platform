package com.example.project01.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh token payload used to obtain a new access token.
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refresh token must not be blank")
    private String refreshToken;
}
