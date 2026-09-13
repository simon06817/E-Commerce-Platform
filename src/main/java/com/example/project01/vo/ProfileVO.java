package com.example.project01.vo;

import com.example.project01.common.AuthRole;
import lombok.Data;

/**
 * Current user profile without password data.
 */
@Data
public class ProfileVO {

    private Long id;

    private String username;

    private AuthRole role;

    private String nickname;

    private String shopName;

    private String phone;

    private String email;

    private String address;
}
