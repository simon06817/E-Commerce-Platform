package com.ecommerce.platform.controller;

import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.common.Result;
import com.ecommerce.platform.dto.PasswordChangeRequest;
import com.ecommerce.platform.dto.ProfileUpdateRequest;
import com.ecommerce.platform.service.ProfileService;
import com.ecommerce.platform.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service profile endpoints for buyer and seller accounts.
 */
@Tag(name = "Profile", description = "buyer and seller profile management")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Validated
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get my profile")
    @GetMapping
    public Result<ProfileVO> get(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(profileService.getProfile(loginUser));
    }

    @Operation(summary = "Update my profile")
    @PutMapping
    public Result<ProfileVO> update(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestBody @Valid ProfileUpdateRequest request) {
        return Result.success(profileService.updateProfile(loginUser, request));
    }

    @Operation(summary = "Change my password")
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestBody @Valid PasswordChangeRequest request) {
        profileService.changePassword(loginUser, request);
        return Result.success();
    }
}
