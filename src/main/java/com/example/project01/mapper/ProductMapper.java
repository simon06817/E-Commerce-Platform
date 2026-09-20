package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.Product;
import com.example.project01.vo.ProductRecommendationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for products.
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("""
            SELECT p.id,
                   p.seller_id,
                   COALESCE(seller.shop_name, seller.username) AS seller_name,
                   p.name,
                   p.description,
                   p.price,
                   p.stock,
                   p.category_id,
                   category.name AS category_name,
                   COALESCE(sales.sales_quantity, 0) AS sales_quantity,
                   COALESCE(reviews.review_count, 0) AS review_count,
                   COALESCE(reviews.average_rating, 0) AS average_rating
            FROM product p
            LEFT JOIN user_seller seller ON seller.id = p.seller_id
            LEFT JOIN product_category category ON category.id = p.category_id
            LEFT JOIN (
                SELECT oi.product_id, SUM(oi.quantity) AS sales_quantity
                FROM order_item oi
                JOIN order_info o ON o.id = oi.order_id
                WHERE o.status IN (1, 2, 3)
                GROUP BY oi.product_id
            ) sales ON sales.product_id = p.id
            LEFT JOIN (
                SELECT product_id,
                       COUNT(*) AS review_count,
                       ROUND(AVG(rating), 1) AS average_rating
                FROM product_review
                GROUP BY product_id
            ) reviews ON reviews.product_id = p.id
            WHERE p.status = 1
              AND (#{categoryId} IS NULL OR p.category_id = #{categoryId})
            ORDER BY sales_quantity DESC,
                     average_rating DESC,
                     review_count DESC,
                     p.create_time DESC
            LIMIT #{limit}
            """)
    List<ProductRecommendationVO> selectRecommendations(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit);
}
