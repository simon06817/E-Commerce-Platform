package com.ecommerce.platform.vo;

import com.ecommerce.platform.common.AuthRole;
import lombok.Data;

/** 用户资料返回对象。 */
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
