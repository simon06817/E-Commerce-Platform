package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for order headers.
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
