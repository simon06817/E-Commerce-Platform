package com.ecommerce.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Aggregated rating information for one product.
 */
@Data
@AllArgsConstructor
public class ReviewSummaryVO {

    private Double averageRating;

    private Long reviewCount;
}
