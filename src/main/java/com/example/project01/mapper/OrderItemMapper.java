package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for order item snapshots.
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
