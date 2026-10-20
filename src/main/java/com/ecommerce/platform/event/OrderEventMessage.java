package com.ecommerce.platform.event;

import java.math.BigDecimal;

/** 事务提交后发布到 RabbitMQ 的订单事件。 */
public record OrderEventMessage(
        Long orderId,
        String orderNo,
        Long buyerId,
        BigDecimal totalAmount,
        String eventType,
        String traceId) {
}
