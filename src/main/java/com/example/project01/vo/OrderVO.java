package com.example.project01.vo;

import com.example.project01.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order view object returned to clients, including item snapshots and a
 * human-readable status description.
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long buyerId;

    private Long sellerId;

    private String sellerName;

    private String checkoutGroupId;

    private String buyerUsername;

    private String buyerNickname;

    private String buyerPhone;

    private String buyerEmail;

    private String buyerAddress;

    private List<String> shopNames;

    private BigDecimal totalAmount;

    private Integer status;

    private String statusText;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime completeTime;

    private List<Long> reviewedItemIds;

    private List<OrderReviewVO> reviews;

    private List<OrderItem> items;
}
