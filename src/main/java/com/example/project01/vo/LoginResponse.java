package com.example.project01.vo;

import com.example.project01.common.AuthRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long userId;

    private String username;

    private AuthRole role;

    private String displayName;
}
