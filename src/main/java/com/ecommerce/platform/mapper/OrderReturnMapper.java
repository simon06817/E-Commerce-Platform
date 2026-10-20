package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.OrderReturn;
import org.apache.ibatis.annotations.Mapper;

/** 退款数据访问接口。 */
@Mapper
public interface OrderReturnMapper extends BaseMapper<OrderReturn> {
}
