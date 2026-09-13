package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for products.
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
