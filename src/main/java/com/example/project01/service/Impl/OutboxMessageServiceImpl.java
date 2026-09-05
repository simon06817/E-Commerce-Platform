package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.OutboxMessage;
import com.example.project01.mapper.OutboxMessageMapper;
import com.example.project01.service.OutboxMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxMessageServiceImpl extends ServiceImpl<OutboxMessageMapper, OutboxMessage>
        implements OutboxMessageService {

    @Override
    public void savePending(Long orderId, String eventType, String payload) {
        OutboxMessage message = new OutboxMessage();
        message.setOrderId(orderId);
        message.setEventType(eventType);
        message.setPayload(payload);
        message.setStatus(0);
        message.setRetryCount(0);
        save(message);
    }

    @Override
    public List<OutboxMessage> listPending(int limit) {
        return lambdaQuery()
                .eq(OutboxMessage::getStatus, 0)
                .orderByAsc(OutboxMessage::getCreateTime)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public void markSent(Long id) {
        update(new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .set(OutboxMessage::getStatus, 1));
    }

    @Override
    public void markRetry(Long id) {
        update(new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .setSql("retry_count = retry_count + 1"));
    }
}
