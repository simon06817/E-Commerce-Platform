package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.UserSeller;

/** 卖家账号服务。 */
public interface UserSellerService extends IService<UserSeller> {

    /** 根据用户名查询账号。 */
    UserSeller getByUsername(String username);

    /** 判断用户名是否已存在。 */
    boolean existsByUsername(String username);

    /** 按关键词分页查询账号。 */
    Page<UserSeller> pageWithKeyword(int current, int size, String keyword);
}
