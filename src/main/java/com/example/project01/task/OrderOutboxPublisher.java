package com.example.project01.task;

import com.example.project01.entity.OutboxMessage;
import com.example.project01.service.OutboxMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderOutboxPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxMessageService outboxMessageService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.order-exchange}")
    private String exchange;

    @Value("${app.rabbitmq.order-routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.order-paid-routing-key}")
    private String paidRoutingKey;

    @Scheduled(fixedDelayString = "${app.outbox.scan-interval-ms:5000}")
    public void publishPending() {
        List<OutboxMessage> pending = outboxMessageService.listPending(BATCH_SIZE);
        for (OutboxMessage message : pending) {
            try {
                String key = "ORDER_PAID".equals(message.getEventType()) ? paidRoutingKey : routingKey;
                rabbitTemplate.convertAndSend(exchange, key, message);
                outboxMessageService.markSent(message.getId());
            } catch (AmqpException e) {
                outboxMessageService.markRetry(message.getId());
                log.warn("publish outbox message failed, id={}", message.getId(), e);
            }
        }
    }
}
