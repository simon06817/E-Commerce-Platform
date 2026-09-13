package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for order headers.
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
