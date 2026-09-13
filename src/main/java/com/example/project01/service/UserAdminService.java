package com.example.project01.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.UserAdmin;

/**
 * Administrator account queries.
 */
public interface UserAdminService extends IService<UserAdmin> {

    UserAdmin getByUsername(String username);
}
