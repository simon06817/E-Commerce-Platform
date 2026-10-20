package com.ecommerce.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** 卖家统计返回对象。 */
@Data
@AllArgsConstructor
public class SellerStatsVO {

    private BigDecimal totalRevenue;

    private Long orderCount;

    private Long itemCount;

    private BigDecimal averageOrderAmount;

    private String range;
}
