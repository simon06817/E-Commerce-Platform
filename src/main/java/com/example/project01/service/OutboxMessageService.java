package com.example.project01.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.OutboxMessage;

import java.util.List;

/**
 * Stores and tracks outbox messages until they are acknowledged by the broker.
 */
public interface OutboxMessageService extends IService<OutboxMessage> {

    void savePending(Long orderId, String eventType, String payload);

    List<OutboxMessage> listPending(int limit);

    void markSent(Long id);

    void markRetry(Long id);
}
