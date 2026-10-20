package com.ecommerce.platform.service;

import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.dto.PasswordChangeRequest;
import com.ecommerce.platform.dto.ProfileUpdateRequest;
import com.ecommerce.platform.vo.ProfileVO;

/** 用户资料服务。 */
public interface ProfileService {

    /** 查询当前用户资料。 */
    ProfileVO getProfile(LoginUser loginUser);

    /** 修改当前用户资料。 */
    ProfileVO updateProfile(LoginUser loginUser, ProfileUpdateRequest request);

    /** 修改当前用户密码。 */
    void changePassword(LoginUser loginUser, PasswordChangeRequest request);
}
