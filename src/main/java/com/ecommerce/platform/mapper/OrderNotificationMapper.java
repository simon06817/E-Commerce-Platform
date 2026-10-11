package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.OrderNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for order notifications.
 */
@Mapper
public interface OrderNotificationMapper extends BaseMapper<OrderNotification> {
}
