package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.Order;

public interface OrderService extends IService<Order> {

    //分页查询订单列表
    Page<Order> getOrderPage(int current, int size, Long userId, String status);

    //创建订单
    Order createOrder(Long userId, String  address);

    //取消订单
    void cancelOrder(Long orderId);

    //支付订单
    void payOrder(Long orderId);

}
