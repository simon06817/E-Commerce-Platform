package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.OrderNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for order notifications.
 */
@Mapper
public interface OrderNotificationMapper extends BaseMapper<OrderNotification> {
}
