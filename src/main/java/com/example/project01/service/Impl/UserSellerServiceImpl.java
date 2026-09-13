package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.UserSeller;
import com.example.project01.mapper.UserSellerMapper;
import com.example.project01.service.UserSellerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default seller account service.
 */
@Service
public class UserSellerServiceImpl extends ServiceImpl<UserSellerMapper, UserSeller> implements UserSellerService {

    @Override
    public UserSeller getByUsername(String username) {
        return lambdaQuery().eq(UserSeller::getUsername, username).one();
    }

    @Override
    public boolean existsByUsername(String username) {
        return lambdaQuery().eq(UserSeller::getUsername, username).count() > 0;
    }

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
