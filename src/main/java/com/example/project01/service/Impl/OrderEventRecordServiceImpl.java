package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.OrderEventRecord;
import com.example.project01.mapper.OrderEventRecordMapper;
import com.example.project01.service.OrderEventRecordService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Deduplicates consumed order events using the unique (order_id, event_type) key.
 */
@Service
public class OrderEventRecordServiceImpl extends ServiceImpl<OrderEventRecordMapper, OrderEventRecord>
        implements OrderEventRecordService {

    @Override
    public boolean recordIfAbsent(Long orderId, String eventType, String payload,
                                  String traceId) {
        if (lambdaQuery()
                .eq(OrderEventRecord::getOrderId, orderId)
                .eq(OrderEventRecord::getEventType, eventType)
                .count() > 0) {
            return false;
        }
        OrderEventRecord record = new OrderEventRecord();
        record.setOrderId(orderId);
        record.setEventType(eventType);
        record.setPayload(payload);
        record.setTraceId(traceId);
        try {
            save(record);
            return true;
        } catch (DuplicateKeyException ignored) {
            // concurrent duplicate consumer
            return false;
        }
    }
}
