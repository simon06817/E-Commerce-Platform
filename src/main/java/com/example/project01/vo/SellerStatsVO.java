package com.example.project01.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Seller revenue statistics for a selected time range.
 */
@Data
@AllArgsConstructor
public class SellerStatsVO {

    private BigDecimal totalRevenue;

    private Long orderCount;

    private Long itemCount;

    private BigDecimal averageOrderAmount;

    private String range;
}
