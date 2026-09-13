package com.example.project01.service.Impl;

import com.example.project01.common.AuthRole;
import com.example.project01.common.BusinessException;
import com.example.project01.common.LoginUser;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.PasswordChangeRequest;
import com.example.project01.dto.ProfileUpdateRequest;
import com.example.project01.entity.UserBuyer;
import com.example.project01.entity.UserSeller;
import com.example.project01.service.ProfileService;
import com.example.project01.service.UserBuyerService;
import com.example.project01.service.UserSellerService;
import com.example.project01.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile service. Username changes are intentionally not supported.
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserBuyerService userBuyerService;
    private final UserSellerService userSellerService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileVO getProfile(LoginUser loginUser) {
        if (loginUser.getRole() == AuthRole.BUYER) {
            return toBuyerVO(userBuyerService.getById(loginUser.getId()));
        }
        if (loginUser.getRole() == AuthRole.SELLER) {
            return toSellerVO(userSellerService.getById(loginUser.getId()));
        }
        throw new BusinessException(ResultCode.INVALID_ROLE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileVO updateProfile(LoginUser loginUser, ProfileUpdateRequest request) {
        if (loginUser.getRole() == AuthRole.BUYER) {
            UserBuyer buyer = userBuyerService.getById(loginUser.getId());
            if (buyer == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST);
            }
            buyer.setNickname(request.getNickname());
            buyer.setPhone(request.getPhone());
            buyer.setEmail(request.getEmail());
            buyer.setAddress(request.getAddress());
            userBuyerService.updateById(buyer);
            return toBuyerVO(buyer);
        }
        if (loginUser.getRole() == AuthRole.SELLER) {
            UserSeller seller = userSellerService.getById(loginUser.getId());
            if (seller == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST);
            }
            seller.setShopName(request.getShopName());
            seller.setPhone(request.getPhone());
            seller.setEmail(request.getEmail());
            userSellerService.updateById(seller);
            return toSellerVO(seller);
        }
        throw new BusinessException(ResultCode.INVALID_ROLE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(LoginUser loginUser, PasswordChangeRequest request) {
        String encoded = passwordEncoder.encode(request.getNewPassword());
        if (loginUser.getRole() == AuthRole.BUYER) {
            UserBuyer buyer = userBuyerService.getById(loginUser.getId());
            verifyAndSetPassword(buyer == null ? null : buyer.getPassword(), request.getOldPassword(), encoded);
            buyer.setPassword(encoded);
            userBuyerService.updateById(buyer);
            return;
        }
        if (loginUser.getRole() == AuthRole.SELLER) {
            UserSeller seller = userSellerService.getById(loginUser.getId());
            verifyAndSetPassword(seller == null ? null : seller.getPassword(), request.getOldPassword(), encoded);
            seller.setPassword(encoded);
            userSellerService.updateById(seller);
            return;
        }
        throw new BusinessException(ResultCode.INVALID_ROLE);
    }

    private void verifyAndSetPassword(String stored, String oldPassword, String encoded) {
        if (stored == null || !passwordEncoder.matches(oldPassword, stored)) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }

    private ProfileVO toBuyerVO(UserBuyer buyer) {
        if (buyer == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        ProfileVO vo = new ProfileVO();
        vo.setId(buyer.getId());
        vo.setUsername(buyer.getUsername());
        vo.setRole(AuthRole.BUYER);
        vo.setNickname(buyer.getNickname());
        vo.setPhone(buyer.getPhone());
        vo.setEmail(buyer.getEmail());
        vo.setAddress(buyer.getAddress());
        return vo;
    }

    private ProfileVO toSellerVO(UserSeller seller) {
        if (seller == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        ProfileVO vo = new ProfileVO();
        vo.setId(seller.getId());
        vo.setUsername(seller.getUsername());
        vo.setRole(AuthRole.SELLER);
        vo.setShopName(seller.getShopName());
        vo.setPhone(seller.getPhone());
        vo.setEmail(seller.getEmail());
        return vo;
    }
}
