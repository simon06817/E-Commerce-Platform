package com.example.project01.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authenticated principal stored in the Spring Security context. It is built
 * from JWT claims and is used by controllers via {@code @AuthenticationPrincipal}.
 */
@Data
@NoArgsConstructor
public class LoginUser {

    private Long id;

    private String username;

    private AuthRole role;

    @JsonIgnore
    private String jti;

    public LoginUser(Long id, String username, AuthRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
}
