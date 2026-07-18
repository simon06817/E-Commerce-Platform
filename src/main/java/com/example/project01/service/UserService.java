package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.User;

public interface UserService extends IService<User> {

    //分页查询用户列表
    Page<User> getUserPage(int current,int size,String keyword);

    //用户注册
    void register(User user);

    //用户登录
    User login(String username,String password);

    //更新用户信息
    void updateUser(User user);

}
