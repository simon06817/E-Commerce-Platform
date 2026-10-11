package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.OrderReturn;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for return/refund requests.
 */
@Mapper
public interface OrderReturnMapper extends BaseMapper<OrderReturn> {
}
