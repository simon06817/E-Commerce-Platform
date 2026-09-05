package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.dto.OrderCreateRequest;
import com.example.project01.entity.Order;
import com.example.project01.vo.OrderVO;

public interface OrderService extends IService<Order> {

    Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status);

    OrderVO getOrderDetail(Long buyerId, Long orderId);

    Order createOrder(Long buyerId, OrderCreateRequest request);

    void cancelOrder(Long buyerId, Long orderId);

    void payOrder(Long buyerId, Long orderId);

    int closeExpiredOrders(int expireMinutes);

    Page<Order> getSellerOrderPage(Long sellerId, int current, int size, Integer status);

    OrderVO getSellerOrderDetail(Long sellerId, Long orderId);

    void shipOrder(Long sellerId, Long orderId);

    void confirmOrder(Long buyerId, Long orderId);
}
