package com.ecommerce.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 评价汇总返回对象。 */
@Data
@AllArgsConstructor
public class ReviewSummaryVO {

    private Double averageRating;

    private Long reviewCount;
}
