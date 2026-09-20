package com.example.project01.task;

import com.example.project01.entity.OutboxMessage;
import com.example.project01.observability.BusinessMetrics;
import com.example.project01.observability.TraceContext;
import com.example.project01.service.OutboxMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled publisher that moves local outbox messages to RabbitMQ and marks
 * them as sent. Failed sends are retried in later scans.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderOutboxPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxMessageService outboxMessageService;
    private final RabbitTemplate rabbitTemplate;
    private final BusinessMetrics businessMetrics;

    @Value("${app.rabbitmq.order-exchange}")
    private String exchange;

    @Value("${app.rabbitmq.order-routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.order-paid-routing-key}")
    private String paidRoutingKey;

    @Scheduled(fixedDelayString = "${app.outbox.scan-interval-ms:5000}")
    public void publishPending() {
        updateBacklogMetrics();
        List<OutboxMessage> pending = outboxMessageService.listPending(BATCH_SIZE);
        for (OutboxMessage message : pending) {
            String traceId = TraceContext.normalizeOrCreate(message.getTraceId());
            message.setTraceId(traceId);
            try {
                String key = "ORDER_PAID".equals(message.getEventType()) ? paidRoutingKey : routingKey;
                try (MDC.MDCCloseable ignored = TraceContext.withTraceId(traceId)) {
                    rabbitTemplate.convertAndSend(exchange, key, message);
                }
                outboxMessageService.markSent(message.getId());
                businessMetrics.recordOutboxPublish(message.getEventType(), true);
            } catch (Exception e) {
                outboxMessageService.markRetry(message.getId());
                businessMetrics.recordOutboxPublish(message.getEventType(), false);
                log.warn("publish outbox message failed, id={}, traceId={}",
                        message.getId(), traceId, e);
            }
        }
    }

    private void updateBacklogMetrics() {
        long pendingCount = outboxMessageService.lambdaQuery()
                .eq(OutboxMessage::getStatus, com.example.project01.common.OutboxStatusEnum
                        .PENDING.getCode())
                .count();
        OutboxMessage oldest = outboxMessageService.lambdaQuery()
                .eq(OutboxMessage::getStatus, com.example.project01.common.OutboxStatusEnum
                        .PENDING.getCode())
                .orderByAsc(OutboxMessage::getCreateTime)
                .last("LIMIT 1")
                .one();
        long ageSeconds = oldest == null || oldest.getCreateTime() == null
                ? 0 : Math.max(0, Duration.between(oldest.getCreateTime(), LocalDateTime.now())
                        .getSeconds());
        businessMetrics.updateOutboxBacklog(pendingCount, ageSeconds);
    }
}
