package com.ecommerce.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 订单评价返回对象。 */
@Data
public class OrderReviewVO {

    private Long id;

    private Long orderItemId;

    private Integer rating;

    private String content;

    private String replyContent;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
