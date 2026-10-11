package com.ecommerce.platform.service;

import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.dto.PasswordChangeRequest;
import com.ecommerce.platform.dto.ProfileUpdateRequest;
import com.ecommerce.platform.vo.ProfileVO;

/**
 * Self-service profile and password management for buyers and sellers.
 */
public interface ProfileService {

    ProfileVO getProfile(LoginUser loginUser);

    ProfileVO updateProfile(LoginUser loginUser, ProfileUpdateRequest request);

    void changePassword(LoginUser loginUser, PasswordChangeRequest request);
}
