package com.example.project01.service;

import com.example.project01.entity.Order;

public interface OrderEventService {

    void publishOrderCreated(Order order);

    void publishOrderPaid(Order order);
}
