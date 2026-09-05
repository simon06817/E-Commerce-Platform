package com.example.project01.task;

import com.example.project01.entity.OutboxMessage;
import com.example.project01.service.OrderEventRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderCreatedEventListener {

    private final OrderEventRecordService orderEventRecordService;

    @RabbitListener(queues = "${app.rabbitmq.order-queue}")
    public void onOrderCreated(OutboxMessage message) {
        orderEventRecordService.recordIfAbsent(
                message.getOrderId(), message.getEventType(), message.getPayload());
        log.info("order event consumed and recorded, orderId={}, event={}",
                message.getOrderId(), message.getEventType());
    }
}
