package com.ecommerce.platform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 退款返回对象。 */
@Data
public class ReturnVO {

    private Long id;

    private Long orderId;

    private String orderNo;

    private Long orderItemId;

    private Long productId;

    private String productName;

    private Integer quantity;

    private String reason;

    private Integer status;

    private String statusText;

    private BigDecimal refundAmount;

    private String handleNote;

    private LocalDateTime applyTime;

    private LocalDateTime handleTime;
}
