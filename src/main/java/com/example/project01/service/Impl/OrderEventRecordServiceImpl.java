package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.OrderEventRecord;
import com.example.project01.mapper.OrderEventRecordMapper;
import com.example.project01.service.OrderEventRecordService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class OrderEventRecordServiceImpl extends ServiceImpl<OrderEventRecordMapper, OrderEventRecord>
        implements OrderEventRecordService {

    @Override
    public void recordIfAbsent(Long orderId, String eventType, String payload) {
        if (lambdaQuery()
                .eq(OrderEventRecord::getOrderId, orderId)
                .eq(OrderEventRecord::getEventType, eventType)
                .count() > 0) {
            return;
        }
        OrderEventRecord record = new OrderEventRecord();
        record.setOrderId(orderId);
        record.setEventType(eventType);
        record.setPayload(payload);
        try {
            save(record);
        } catch (DuplicateKeyException ignored) {
            // concurrent duplicate consumer
        }
    }
}
