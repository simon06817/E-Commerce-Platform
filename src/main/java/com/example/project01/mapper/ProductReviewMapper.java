package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for product reviews.
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
}
