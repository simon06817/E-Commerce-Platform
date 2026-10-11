package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.OrderEventRecord;

/**
 * Idempotent persistence of consumed order events.
 */
public interface OrderEventRecordService extends IService<OrderEventRecord> {

    boolean recordIfAbsent(Long orderId, String eventType, String payload,
                           String traceId);
}
