package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.OrderEventRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for consumed event records.
 */
@Mapper
public interface OrderEventRecordMapper extends BaseMapper<OrderEventRecord> {
}
