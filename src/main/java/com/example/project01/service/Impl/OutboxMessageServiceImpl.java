package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.OutboxStatusEnum;
import com.example.project01.entity.OutboxMessage;
import com.example.project01.mapper.OutboxMessageMapper;
import com.example.project01.service.OutboxMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Persists and tracks pending outbox messages.
 */
@Service
public class OutboxMessageServiceImpl extends ServiceImpl<OutboxMessageMapper, OutboxMessage>
        implements OutboxMessageService {

    @Override
    public void savePending(Long orderId, String eventType, String payload) {
        // Written in the same transaction as the order, so an event cannot be lost.
        OutboxMessage message = new OutboxMessage();
        message.setOrderId(orderId);
        message.setEventType(eventType);
        message.setPayload(payload);
        message.setStatus(OutboxStatusEnum.PENDING.getCode());
        message.setRetryCount(0);
        save(message);
    }

    @Override
    public List<OutboxMessage> listPending(int limit) {
        // Publisher polls pending rows and marks them sent after broker ack.
        return lambdaQuery()
                .eq(OutboxMessage::getStatus, OutboxStatusEnum.PENDING.getCode())
                .orderByAsc(OutboxMessage::getCreateTime)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public void markSent(Long id) {
        update(new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .set(OutboxMessage::getStatus, OutboxStatusEnum.SENT.getCode()));
    }

    @Override
    public void markRetry(Long id) {
        update(new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .setSql("retry_count = retry_count + 1"));
    }
}
