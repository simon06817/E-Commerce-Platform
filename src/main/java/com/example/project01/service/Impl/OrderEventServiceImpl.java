package com.example.project01.service.Impl;

import com.example.project01.entity.Order;
import com.example.project01.service.OrderEventService;
import com.example.project01.service.OutboxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventServiceImpl implements OrderEventService {

    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_ORDER_PAID = "ORDER_PAID";

    private final OutboxMessageService outboxMessageService;
    private final ObjectMapper objectMapper;

    @Override
    public void publishOrderCreated(Order order) {
        publish(order, EVENT_ORDER_CREATED);
    }

    @Override
    public void publishOrderPaid(Order order) {
        publish(order, EVENT_ORDER_PAID);
    }

    private void publish(Order order, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("buyerId", order.getBuyerId());
        payload.put("totalAmount", order.getTotalAmount());
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxMessageService.savePending(order.getId(), eventType, json);
        } catch (JsonProcessingException e) {
            log.error("serialize order event payload failed, orderId={}", order.getId(), e);
        }
    }
}
