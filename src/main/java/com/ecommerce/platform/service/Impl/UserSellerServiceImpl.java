package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.entity.UserSeller;
import com.ecommerce.platform.mapper.UserSellerMapper;
import com.ecommerce.platform.service.UserSellerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 卖家账号服务实现。 */
@Service
public class UserSellerServiceImpl extends ServiceImpl<UserSellerMapper, UserSeller> implements UserSellerService {

    /** 根据用户名查询账号。 */
    @Override
    public UserSeller getByUsername(String username) {
        return lambdaQuery().eq(UserSeller::getUsername, username).one();
    }

    /** 判断用户名是否已存在。 */
    @Override
    public boolean existsByUsername(String username) {
        return lambdaQuery().eq(UserSeller::getUsername, username).count() > 0;
    }

    /** 按关键词分页查询账号。 */
    @Override
    public Page<UserSeller> pageWithKeyword(int current, int size, String keyword) {
        Page<UserSeller> page = new Page<>(current, size);
        LambdaQueryWrapper<UserSeller> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserSeller::getUsername, keyword)
                    .or()
                    .like(UserSeller::getShopName, keyword)
                    .or()
                    .like(UserSeller::getPhone, keyword);
        }
        wrapper.orderByDesc(UserSeller::getCreateTime);
        return page(page, wrapper);
    }
}
