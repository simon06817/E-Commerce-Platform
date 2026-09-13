package com.example.project01.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Editable profile fields for buyers and sellers.
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 50, message = "nickname length cannot exceed 50")
    private String nickname;

    @Size(max = 100, message = "shop name length cannot exceed 100")
    private String shopName;

    @Size(max = 20, message = "phone length cannot exceed 20")
    private String phone;

    @Email(message = "email format is invalid")
    private String email;

    @Size(max = 255, message = "address length cannot exceed 255")
    private String address;
}
