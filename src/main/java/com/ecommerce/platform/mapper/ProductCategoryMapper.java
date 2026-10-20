package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/** 商品分类数据访问接口。 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
