package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for product reviews.
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
}
