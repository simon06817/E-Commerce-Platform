package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.UserBuyer;
import com.example.project01.mapper.UserBuyerMapper;
import com.example.project01.service.UserBuyerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default buyer account service.
 */
@Service
public class UserBuyerServiceImpl extends ServiceImpl<UserBuyerMapper, UserBuyer> implements UserBuyerService {

    @Override
    public UserBuyer getByUsername(String username) {
        return lambdaQuery().eq(UserBuyer::getUsername, username).one();
    }

    @Override
    public boolean existsByUsername(String username) {
        return lambdaQuery().eq(UserBuyer::getUsername, username).count() > 0;
    }

    @Override
    public Page<UserBuyer> pageWithKeyword(int current, int size, String keyword) {
        Page<UserBuyer> page = new Page<>(current, size);
        LambdaQueryWrapper<UserBuyer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserBuyer::getUsername, keyword)
                    .or()
                    .like(UserBuyer::getNickname, keyword)
                    .or()
                    .like(UserBuyer::getPhone, keyword);
        }
        wrapper.orderByDesc(UserBuyer::getCreateTime);
        return page(page, wrapper);
    }
}
