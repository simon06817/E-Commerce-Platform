package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.OrderEventRecord;

/** 订单事件消费记录服务。 */
public interface OrderEventRecordService extends IService<OrderEventRecord> {

    /** 幂等记录已消费事件。 */
    boolean recordIfAbsent(Long orderId, String eventType, String payload,
                           String traceId);
}
