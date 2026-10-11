package com.ecommerce.platform.service.Impl;

import com.ecommerce.platform.common.AuthRole;
import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.common.ResultCode;
import com.ecommerce.platform.dto.PasswordChangeRequest;
import com.ecommerce.platform.dto.ProfileUpdateRequest;
import com.ecommerce.platform.entity.UserBuyer;
import com.ecommerce.platform.entity.UserAdmin;
import com.ecommerce.platform.entity.UserSeller;
import com.ecommerce.platform.service.ProfileService;
import com.ecommerce.platform.service.UserAdminService;
import com.ecommerce.platform.service.UserBuyerService;
import com.ecommerce.platform.service.UserSellerService;
import com.ecommerce.platform.vo.ProfileVO;
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
    private final UserAdminService userAdminService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileVO getProfile(LoginUser loginUser) {
        if (loginUser.getRole() == AuthRole.BUYER) {
            return toBuyerVO(userBuyerService.getById(loginUser.getId()));
        }
        if (loginUser.getRole() == AuthRole.SELLER) {
            return toSellerVO(userSellerService.getById(loginUser.getId()));
        }
        if (loginUser.getRole() == AuthRole.ADMIN) {
            return toAdminVO(userAdminService.getById(loginUser.getId()));
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
        if (loginUser.getRole() == AuthRole.ADMIN) {
            UserAdmin admin = userAdminService.getById(loginUser.getId());
            if (admin == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST);
            }
            admin.setPhone(request.getPhone());
            admin.setEmail(request.getEmail());
            userAdminService.updateById(admin);
            return toAdminVO(admin);
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
        if (loginUser.getRole() == AuthRole.ADMIN) {
            UserAdmin admin = userAdminService.getById(loginUser.getId());
            verifyAndSetPassword(admin == null ? null : admin.getPassword(), request.getOldPassword(), encoded);
            admin.setPassword(encoded);
            userAdminService.updateById(admin);
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

    private ProfileVO toAdminVO(UserAdmin admin) {
        if (admin == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        ProfileVO vo = new ProfileVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRole(AuthRole.ADMIN);
        vo.setPhone(admin.getPhone());
        vo.setEmail(admin.getEmail());
        return vo;
    }
}
