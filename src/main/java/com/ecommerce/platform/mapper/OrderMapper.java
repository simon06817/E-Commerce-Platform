package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/** 订单数据访问接口。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
