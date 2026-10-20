package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.entity.OrderEventRecord;
import com.ecommerce.platform.mapper.OrderEventRecordMapper;
import com.ecommerce.platform.service.OrderEventRecordService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 订单事件消费记录服务实现。 */
@Service
public class OrderEventRecordServiceImpl extends ServiceImpl<OrderEventRecordMapper, OrderEventRecord>
        implements OrderEventRecordService {

    /** 幂等记录已消费事件。 */
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
            // 并发重复消费
            return false;
        }
    }
}
