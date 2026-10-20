package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.OrderNotification;

/** 订单通知服务。 */
public interface OrderNotificationService extends IService<OrderNotification> {

    /** 幂等创建订单通知。 */
    void createIfAbsent(Long orderId, String eventType, Long recipientId, String recipientRole,
                        String title, String content);

    /** 查询当前用户通知。 */
    Page<OrderNotification> getMyNotifications(String recipientRole, Long recipientId,
                                               int current, int size);

    /** 将通知标记为已读。 */
    void markRead(String recipientRole, Long recipientId, Long notificationId);
}
