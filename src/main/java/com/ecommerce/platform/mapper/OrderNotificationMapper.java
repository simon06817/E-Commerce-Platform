package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.OrderNotification;
import org.apache.ibatis.annotations.Mapper;

/** 订单通知数据访问接口。 */
@Mapper
public interface OrderNotificationMapper extends BaseMapper<OrderNotification> {
}
