package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.UserAdmin;
import com.example.project01.mapper.UserAdminMapper;
import com.example.project01.service.UserAdminService;
import org.springframework.stereotype.Service;

@Service
public class UserAdminServiceImpl extends ServiceImpl<UserAdminMapper, UserAdmin> implements UserAdminService {

    @Override
    public UserAdmin getByUsername(String username) {
        return lambdaQuery().eq(UserAdmin::getUsername, username).one();
    }
}
