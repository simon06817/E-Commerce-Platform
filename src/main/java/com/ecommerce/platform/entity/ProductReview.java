package com.ecommerce.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品评价实体。 */
@Data
@TableName("product_review")
public class ProductReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long buyerId;

    private Long orderId;

    private Long orderItemId;

    private Integer rating;

    private String content;

    private String replyContent;

    private LocalDateTime replyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
