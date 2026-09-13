package com.example.project01.service;

import com.example.project01.common.LoginUser;
import com.example.project01.dto.PasswordChangeRequest;
import com.example.project01.dto.ProfileUpdateRequest;
import com.example.project01.vo.ProfileVO;

/**
 * Self-service profile and password management for buyers and sellers.
 */
public interface ProfileService {

    ProfileVO getProfile(LoginUser loginUser);

    ProfileVO updateProfile(LoginUser loginUser, ProfileUpdateRequest request);

    void changePassword(LoginUser loginUser, PasswordChangeRequest request);
}
