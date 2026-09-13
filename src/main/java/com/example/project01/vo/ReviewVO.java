package com.example.project01.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public review representation with a masked buyer name.
 */
@Data
public class ReviewVO {

    private Long id;

    private Long productId;

    private Integer rating;

    private String content;

    private String replyContent;

    private LocalDateTime replyTime;

    private String buyerName;

    private LocalDateTime createTime;
}
