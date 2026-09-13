package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.OrderReturn;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for return/refund requests.
 */
@Mapper
public interface OrderReturnMapper extends BaseMapper<OrderReturn> {
}
