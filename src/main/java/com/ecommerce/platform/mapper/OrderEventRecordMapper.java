package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.OrderEventRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for consumed event records.
 */
@Mapper
public interface OrderEventRecordMapper extends BaseMapper<OrderEventRecord> {
}
