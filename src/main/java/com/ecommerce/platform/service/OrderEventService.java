package com.ecommerce.platform.service;

import com.ecommerce.platform.entity.Order;

/** 订单事件服务。 */
public interface OrderEventService {

    /** 发布订单创建事件。 */
    void publishOrderCreated(Order order);

    /** 发布订单支付事件。 */
    void publishOrderPaid(Order order);
}
