package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.OrderNotification;
import com.example.project01.mapper.OrderNotificationMapper;
import com.example.project01.service.OrderNotificationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification service with a unique-key based idempotency guard.
 */
@Service
public class OrderNotificationServiceImpl extends ServiceImpl<OrderNotificationMapper, OrderNotification>
        implements OrderNotificationService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createIfAbsent(Long orderId, String eventType, Long recipientId, String recipientRole,
                               String title, String content) {
        if (lambdaQuery()
                .eq(OrderNotification::getOrderId, orderId)
                .eq(OrderNotification::getEventType, eventType)
                .eq(OrderNotification::getRecipientRole, recipientRole)
                .eq(OrderNotification::getRecipientId, recipientId)
                .count() > 0) {
            return;
        }
        OrderNotification notification = new OrderNotification();
        notification.setOrderId(orderId);
        notification.setEventType(eventType);
        notification.setRecipientId(recipientId);
        notification.setRecipientRole(recipientRole);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(false);
        try {
            save(notification);
        } catch (DuplicateKeyException ignored) {
            // Concurrent duplicate delivery.
        }
    }

    @Override
    public Page<OrderNotification> getMyNotifications(String recipientRole, Long recipientId,
                                                      int current, int size) {
        return lambdaQuery()
                .eq(OrderNotification::getRecipientRole, recipientRole)
                .eq(OrderNotification::getRecipientId, recipientId)
                .orderByDesc(OrderNotification::getCreateTime)
                .page(new Page<>(current, size));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String recipientRole, Long recipientId, Long notificationId) {
        OrderNotification notification = getById(notificationId);
        if (notification == null
                || !recipientRole.equals(notification.getRecipientRole())
                || !recipientId.equals(notification.getRecipientId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        notification.setReadFlag(true);
        updateById(notification);
    }
}
