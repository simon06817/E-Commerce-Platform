package com.ecommerce.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 订单通知实体。 */
@Data
@TableName("order_notification")
public class OrderNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String eventType;

    private Long recipientId;

    private String recipientRole;

    private String title;

    private String content;

    @TableField("is_read")
    private Boolean readFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
