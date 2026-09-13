package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.UserBuyer;

/**
 * Buyer account queries and pagination.
 */
public interface UserBuyerService extends IService<UserBuyer> {

    UserBuyer getByUsername(String username);

    boolean existsByUsername(String username);

    Page<UserBuyer> pageWithKeyword(int current, int size, String keyword);
}
