package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Local message table entry used by the outbox pattern. Events are written in
 * the same transaction as the business data and published asynchronously.
 */
@Data
@TableName("order_outbox")
public class OutboxMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String eventType;

    private String payload;

    private String traceId;

    private Integer status;

    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
