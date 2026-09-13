package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for product categories.
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
