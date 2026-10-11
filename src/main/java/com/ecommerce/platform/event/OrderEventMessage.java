package com.ecommerce.platform.event;

import java.math.BigDecimal;

/**
 * Serializable order event published to RabbitMQ after the database transaction commits.
 */
public record OrderEventMessage(
        Long orderId,
        String orderNo,
        Long buyerId,
        BigDecimal totalAmount,
        String eventType,
        String traceId) {
}
