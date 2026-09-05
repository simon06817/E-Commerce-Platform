package com.example.project01.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.OrderEventRecord;

public interface OrderEventRecordService extends IService<OrderEventRecord> {

    void recordIfAbsent(Long orderId, String eventType, String payload);
}
