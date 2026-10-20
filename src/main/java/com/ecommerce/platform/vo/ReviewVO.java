package com.ecommerce.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 商品评价返回对象。 */
@Data
public class ReviewVO {

    private Long id;

    private Long productId;

    private String productName;

    private Integer rating;

    private String content;

    private String replyContent;

    private LocalDateTime replyTime;

    private String buyerName;

    private LocalDateTime createTime;
}
