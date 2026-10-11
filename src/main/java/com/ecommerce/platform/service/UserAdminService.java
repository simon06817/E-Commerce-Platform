package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.UserAdmin;

/**
 * Administrator account queries.
 */
public interface UserAdminService extends IService<UserAdmin> {

    UserAdmin getByUsername(String username);
}
