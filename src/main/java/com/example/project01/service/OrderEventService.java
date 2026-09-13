package com.example.project01.service;

import com.example.project01.entity.Order;

/**
 * Writes order domain events into the local outbox.
 */
public interface OrderEventService {

    void publishOrderCreated(Order order);

    void publishOrderPaid(Order order);
}
