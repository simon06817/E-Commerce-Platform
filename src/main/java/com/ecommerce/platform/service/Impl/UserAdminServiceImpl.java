package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.entity.UserAdmin;
import com.ecommerce.platform.mapper.UserAdminMapper;
import com.ecommerce.platform.service.UserAdminService;
import org.springframework.stereotype.Service;

/** 管理员账号服务实现。 */
@Service
public class UserAdminServiceImpl extends ServiceImpl<UserAdminMapper, UserAdmin> implements UserAdminService {

    /** 根据用户名查询账号。 */
    @Override
    public UserAdmin getByUsername(String username) {
        return lambdaQuery().eq(UserAdmin::getUsername, username).one();
    }
}
