package com.ecommerce.platform.task;

import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.event.OrderEventMessage;
import com.ecommerce.platform.observability.TraceContext;
import com.ecommerce.platform.service.OrderEventRecordService;
import com.ecommerce.platform.service.OrderItemService;
import com.ecommerce.platform.service.OrderNotificationService;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;

import java.util.List;

/** 订单创建事件监听器。 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderCreatedEventListener {

    private static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_ORDER_PAID = "ORDER_PAID";

    private final OrderEventRecordService orderEventRecordService;
    private final OrderNotificationService notificationService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    /** 消费订单事件并保证幂等。 */
    @RabbitListener(queues = "${app.rabbitmq.order-queue}")
    @Transactional(rollbackFor = Exception.class)
    public void onOrderEvent(OrderEventMessage message) {
        String traceId = TraceContext.normalizeOrCreate(message.traceId());
        String payload = payloadJson(message);
        try (MDC.MDCCloseable ignored = TraceContext.withTraceId(traceId)) {
            // 先写入消费记录，重复消息会被唯一键拦截。
            boolean firstDelivery = orderEventRecordService.recordIfAbsent(
                    message.orderId(), message.eventType(), payload, traceId);
            if (!firstDelivery) {
                log.info("duplicate order event ignored, orderId={}, event={}, traceId={}",
                        message.orderId(), message.eventType(), traceId);
                return;
            }
            try {
                if (EVENT_ORDER_CREATED.equals(message.eventType())) {
                    notifyBuyerOrderCreated(message.orderId());
                } else if (EVENT_ORDER_PAID.equals(message.eventType())) {
                    notifyOrderPaid(message.orderId());
                }
            } catch (RuntimeException e) {
                throw e;
            }
            log.info("order event consumed and recorded, orderId={}, event={}, traceId={}",
                    message.orderId(), message.eventType(), traceId);
        }
    }

    /** 序列化事件消息。 */
    private String payloadJson(OrderEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize order event payload", e);
        }
    }

    /** 创建买家订单创建通知。 */
    private void notifyBuyerOrderCreated(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return;
        }
        notificationService.createIfAbsent(
                orderId, EVENT_ORDER_CREATED, order.getBuyerId(), "BUYER",
                "订单已创建",
                "您的订单 " + order.getOrderNo() + " 已创建。");
    }

    /** 创建订单支付通知。 */
    private void notifyOrderPaid(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return;
        }
        notificationService.createIfAbsent(
                orderId, EVENT_ORDER_PAID, order.getBuyerId(), "BUYER",
                "支付成功",
                "您的订单 " + order.getOrderNo() + " 已支付。");

        if (order.getSellerId() == null) {
            List<OrderItem> items = orderItemService.lambdaQuery()
                    .eq(OrderItem::getOrderId, orderId)
                    .list();
            List<Long> productIds = items.stream()
                    .map(OrderItem::getProductId)
                    .distinct()
                    .toList();
            if (productIds.isEmpty()) {
                return;
            }
            productService.listByIds(productIds).stream()
                    .map(Product::getSellerId)
                    .distinct()
                    .forEach(sellerId -> notificationService.createIfAbsent(
                            orderId, EVENT_ORDER_PAID, sellerId, "SELLER",
                            "新订单已付款",
                            "订单 " + order.getOrderNo() + " 已付款。"));
            return;
        }
        notificationService.createIfAbsent(
                orderId, EVENT_ORDER_PAID, order.getSellerId(), "SELLER",
                "新订单已付款",
                "订单 " + order.getOrderNo() + " 已付款。");
    }
}
