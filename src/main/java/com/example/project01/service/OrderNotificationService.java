package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.OrderNotification;

/**
 * Creates and reads idempotent order notifications.
 */
public interface OrderNotificationService extends IService<OrderNotification> {

    void createIfAbsent(Long orderId, String eventType, Long recipientId, String recipientRole,
                        String title, String content);

    Page<OrderNotification> getMyNotifications(String recipientRole, Long recipientId,
                                               int current, int size);

    void markRead(String recipientRole, Long recipientId, Long notificationId);
}
