package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Inbox record proving an MQ event was consumed. The unique key
 * (order_id, event_type) makes message consumption idempotent.
 */
@Data
@TableName("order_event_record")
public class OrderEventRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String eventType;

    private String payload;

    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
