package com.example.project01.dto;

import com.example.project01.common.AuthRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotNull(message = "role must not be null")
    private AuthRole role;

    @NotBlank(message = "username must not be blank")
    @Size(min = 2, max = 20, message = "username length must be 2-20")
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 100, message = "password length must be 6-100")
    private String password;

    private String nickname;

    private String shopName;

    private String phone;

    @Email(message = "email format is invalid")
    private String email;

    private String address;
}
