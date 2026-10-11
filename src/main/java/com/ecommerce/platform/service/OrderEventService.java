package com.ecommerce.platform.service;

import com.ecommerce.platform.entity.Order;

/**
 * Publishes order domain events for after-commit delivery.
 */
public interface OrderEventService {

    void publishOrderCreated(Order order);

    void publishOrderPaid(Order order);
}
