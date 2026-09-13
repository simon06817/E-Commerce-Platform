package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code cart} table.
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
