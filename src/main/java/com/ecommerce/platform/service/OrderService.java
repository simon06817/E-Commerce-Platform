package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.dto.OrderCreateRequest;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.vo.OrderVO;
import com.ecommerce.platform.vo.SellerStatsVO;

import java.util.List;

/** 订单服务。 */
public interface OrderService extends IService<Order> {

    /** 分页查询买家订单。 */
    Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status,
                             Boolean reviewed);

    /** 分页查询买家订单详情。 */
    Page<OrderVO> getOrderPageDetails(Long buyerId, int current, int size, Integer status,
                                      Boolean reviewed);

    /** 查询订单详情。 */
    OrderVO getOrderDetail(Long buyerId, Long orderId);

    /** 创建订单。 */
    List<Order> createOrder(Long buyerId, OrderCreateRequest request);

    /** 取消待付款订单并恢复库存。 */
    void cancelOrder(Long buyerId, Long orderId);

    /** 支付待付款订单。 */
    void payOrder(Long buyerId, Long orderId);

    /** 关闭超时未支付订单。 */
    int closeExpiredOrders(int expireMinutes);

    /** 分页查询卖家订单。 */
    Page<Order> getSellerOrderPage(Long sellerId, int current, int size, Integer status);

    /** 分页查询卖家订单详情。 */
    Page<OrderVO> getSellerOrderPageDetails(Long sellerId, int current, int size, Integer status);

    /** 查询卖家订单详情。 */
    OrderVO getSellerOrderDetail(Long sellerId, Long orderId);

    /** 卖家订单发货。 */
    void shipOrder(Long sellerId, Long orderId);

    /** 确认订单收货。 */
    void confirmOrder(Long buyerId, Long orderId);

    /** 自动完成已发货订单。 */
    int autoCompleteShippedOrders(int hours);

    /** 分页查询管理员订单。 */
    Page<Order> getAdminOrderPage(int current, int size, String orderNo, Long buyerId, Integer status);

    /** 查询管理员订单详情。 */
    OrderVO getAdminOrderDetail(Long orderId);

    /** 强制关闭订单并恢复库存。 */
    void forceCancelOrder(Long orderId);

    /** 查询卖家收入统计。 */
    SellerStatsVO getSellerStats(Long sellerId, String range);
}
