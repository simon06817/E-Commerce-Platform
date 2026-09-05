package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_event_record")
public class OrderEventRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String eventType;

    private String payload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
