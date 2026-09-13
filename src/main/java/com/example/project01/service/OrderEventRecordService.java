package com.example.project01.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.OrderEventRecord;

/**
 * Idempotent persistence of consumed order events.
 */
public interface OrderEventRecordService extends IService<OrderEventRecord> {

    void recordIfAbsent(Long orderId, String eventType, String payload);
}
