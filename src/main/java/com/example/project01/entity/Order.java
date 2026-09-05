package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "order no must not be blank")
    @Size(max = 50, message = "order no length cannot exceed 50")
    @TableField("order_no")
    private String orderNo;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @NotNull(message = "buyer id must not be null")
    private Long buyerId;

    @NotNull(message = "total amount must not be null")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @NotNull(message = "order status must not be null")
    private Integer status;

    @NotBlank(message = "receiver name must not be blank")
    @Size(min = 1, max = 50, message = "receiver name length must be 1-50")
    private String receiverName;

    @NotBlank(message = "receiver phone must not be blank")
    @Size(max = 20, message = "receiver phone length cannot exceed 20")
    private String receiverPhone;

    @NotBlank(message = "receiver address must not be blank")
    @Size(max = 200, message = "receiver address length cannot exceed 200")
    private String receiverAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("payment_time")
    private LocalDateTime payTime;
}
