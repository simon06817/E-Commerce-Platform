package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.UserAdmin;

/** 管理员账号服务。 */
public interface UserAdminService extends IService<UserAdmin> {

    /** 根据用户名查询账号。 */
    UserAdmin getByUsername(String username);
}
