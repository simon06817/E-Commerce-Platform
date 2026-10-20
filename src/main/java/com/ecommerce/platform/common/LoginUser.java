package com.ecommerce.platform.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录用户。 */
@Data
@NoArgsConstructor
public class LoginUser {

    private Long id;

    private String username;

    private AuthRole role;

    @JsonIgnore
    private String jti;

    /** 构造登录用户。 */
    public LoginUser(Long id, String username, AuthRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
}
