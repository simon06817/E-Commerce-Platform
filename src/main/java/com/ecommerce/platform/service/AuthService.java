package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.LoginRequest;
import com.ecommerce.platform.dto.RegisterRequest;
import com.ecommerce.platform.vo.LoginResponse;

/** 认证服务。 */
public interface AuthService {

    /** 用户登录并签发令牌。 */
    LoginResponse login(LoginRequest request);

    /** 注册用户账号。 */
    LoginResponse register(RegisterRequest request);

    /** 刷新访问令牌。 */
    LoginResponse refresh(String refreshToken);

    /** 退出登录并撤销刷新令牌。 */
    void logout(String refreshToken);
}
