package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.OrderEventRecord;
import org.apache.ibatis.annotations.Mapper;

/** 订单事件消费记录数据访问接口。 */
@Mapper
public interface OrderEventRecordMapper extends BaseMapper<OrderEventRecord> {
}
