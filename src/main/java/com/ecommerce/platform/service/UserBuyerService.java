package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.UserBuyer;

/** 买家账号服务。 */
public interface UserBuyerService extends IService<UserBuyer> {

    /** 根据用户名查询账号。 */
    UserBuyer getByUsername(String username);

    /** 判断用户名是否已存在。 */
    boolean existsByUsername(String username);

    /** 按关键词分页查询账号。 */
    Page<UserBuyer> pageWithKeyword(int current, int size, String keyword);
}
