package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.dto.OrderCreateRequest;
import com.example.project01.entity.Order;
import com.example.project01.vo.OrderVO;
import com.example.project01.vo.SellerStatsVO;

import java.util.List;

/**
 * Buyer and seller order use cases, including idempotent creation, payment,
 * cancellation, shipping and receipt confirmation.
 */
public interface OrderService extends IService<Order> {

    Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status,
                             Boolean reviewed);

    OrderVO getOrderDetail(Long buyerId, Long orderId);

    List<Order> createOrder(Long buyerId, OrderCreateRequest request);

    void cancelOrder(Long buyerId, Long orderId);

    void payOrder(Long buyerId, Long orderId);

    int closeExpiredOrders(int expireMinutes);

    Page<Order> getSellerOrderPage(Long sellerId, int current, int size, Integer status);

    OrderVO getSellerOrderDetail(Long sellerId, Long orderId);

    void shipOrder(Long sellerId, Long orderId);

    void confirmOrder(Long buyerId, Long orderId);

    int autoCompleteShippedOrders(int hours);

    Page<Order> getAdminOrderPage(int current, int size, String orderNo, Long buyerId, Integer status);

    OrderVO getAdminOrderDetail(Long orderId);

    void forceCancelOrder(Long orderId);

    SellerStatsVO getSellerStats(Long sellerId, String range);
}
