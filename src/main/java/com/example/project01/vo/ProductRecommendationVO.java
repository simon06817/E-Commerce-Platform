package com.example.project01.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Product recommendation ranked by paid sales and verified-purchase reviews.
 */
@Data
public class ProductRecommendationVO {

    private Long id;

    private Long sellerId;

    private String sellerName;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Long categoryId;

    private String categoryName;

    private Long salesQuantity;

    private Long reviewCount;

    private Double averageRating;
}
