package com.ecommerce.platform.task;

import com.ecommerce.platform.event.OrderEventMessage;
import com.ecommerce.platform.observability.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 订单事件发布器。 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.order-exchange}")
    private String exchange;

    @Value("${app.rabbitmq.order-routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.order-paid-routing-key}")
    private String paidRoutingKey;

    /** 发布订单事件。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(OrderEventMessage event) {
        String traceId = TraceContext.normalizeOrCreate(event.traceId());
        String key = "ORDER_PAID".equals(event.eventType()) ? paidRoutingKey : routingKey;
        try (MDC.MDCCloseable ignored = TraceContext.withTraceId(traceId)) {
            // 订单事务提交后再发送事件，避免回滚后产生幽灵消息。
            rabbitTemplate.convertAndSend(exchange, key, event,
                    new CorrelationData(event.eventType() + ":" + event.orderId()));
            log.info("order event published, orderId={}, event={}, traceId={}",
                    event.orderId(), event.eventType(), traceId);
        } catch (Exception e) {
            log.error("order event publish failed, orderId={}, event={}, traceId={}",
                    event.orderId(), event.eventType(), traceId, e);
        }
    }
}
