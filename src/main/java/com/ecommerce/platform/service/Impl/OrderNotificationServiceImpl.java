package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.common.ResultCode;
import com.ecommerce.platform.entity.OrderNotification;
import com.ecommerce.platform.mapper.OrderNotificationMapper;
import com.ecommerce.platform.service.OrderNotificationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 订单通知服务实现。 */
@Service
public class OrderNotificationServiceImpl extends ServiceImpl<OrderNotificationMapper, OrderNotification>
        implements OrderNotificationService {

    /** 幂等创建订单通知。 */
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
            // 并发重复投递。
        }
    }

    /** 查询当前用户通知。 */
    @Override
    public Page<OrderNotification> getMyNotifications(String recipientRole, Long recipientId,
                                                      int current, int size) {
        return lambdaQuery()
                .eq(OrderNotification::getRecipientRole, recipientRole)
                .eq(OrderNotification::getRecipientId, recipientId)
                .orderByDesc(OrderNotification::getCreateTime)
                .page(new Page<>(current, size));
    }

    /** 将通知标记为已读。 */
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
