package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for product categories.
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
