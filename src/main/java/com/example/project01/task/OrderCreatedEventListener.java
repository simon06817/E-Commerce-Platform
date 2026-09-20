package com.example.project01.task;

import com.example.project01.entity.OutboxMessage;
import com.example.project01.entity.Order;
import com.example.project01.entity.OrderItem;
import com.example.project01.entity.Product;
import com.example.project01.service.OrderEventRecordService;
import com.example.project01.service.OrderItemService;
import com.example.project01.service.OrderNotificationService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RabbitMQ consumer for order events. Records each event once, making message
 * delivery idempotent even when the broker redelivers.
 */
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

    @RabbitListener(queues = "${app.rabbitmq.order-queue}")
    public void onOrderCreated(OutboxMessage message) {
        orderEventRecordService.recordIfAbsent(
                message.getOrderId(), message.getEventType(), message.getPayload());
        if (EVENT_ORDER_CREATED.equals(message.getEventType())) {
            notifyBuyerOrderCreated(message.getOrderId());
        } else if (EVENT_ORDER_PAID.equals(message.getEventType())) {
            notifyOrderPaid(message.getOrderId());
        }
        log.info("order event consumed and recorded, orderId={}, event={}",
                message.getOrderId(), message.getEventType());
    }

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
