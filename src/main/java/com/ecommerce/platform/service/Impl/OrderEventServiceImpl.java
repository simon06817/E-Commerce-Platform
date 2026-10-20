package com.ecommerce.platform.service.Impl;

import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.event.OrderEventMessage;
import com.ecommerce.platform.observability.TraceContext;
import com.ecommerce.platform.service.OrderEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** 订单事件服务实现。 */
@Service
@RequiredArgsConstructor
public class OrderEventServiceImpl implements OrderEventService {

    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_ORDER_PAID = "ORDER_PAID";

    private final ApplicationEventPublisher eventPublisher;

    /** 发布订单创建事件。 */
    @Override
    public void publishOrderCreated(Order order) {
        publish(order, EVENT_ORDER_CREATED);
    }

    /** 发布订单支付事件。 */
    @Override
    public void publishOrderPaid(Order order) {
        publish(order, EVENT_ORDER_PAID);
    }

    /** 发布订单事件。 */
    private void publish(Order order, String eventType) {
        eventPublisher.publishEvent(new OrderEventMessage(
                order.getId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getTotalAmount(),
                eventType,
                TraceContext.currentOrCreate()));
    }
}
