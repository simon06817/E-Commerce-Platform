package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.UserSeller;

public interface UserSellerService extends IService<UserSeller> {

    UserSeller getByUsername(String username);

    boolean existsByUsername(String username);

    Page<UserSeller> pageWithKeyword(int current, int size, String keyword);
}
