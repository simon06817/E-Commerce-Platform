package com.example.project01.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Review information attached to an order item in the buyer order detail.
 */
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
