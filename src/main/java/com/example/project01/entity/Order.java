package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class Order {
    @TableId(type= IdType.AUTO)
    private Long id;

    @NotBlank(message = "订单号不能为空")
    @Size(max = 50, message = "订单号长度不能超过50个字符")
    private String orderId; //订单号

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "订单总金额不能为空")
    @DecimalMin(value = "0.01", message = "订单总金额必须大于0")
    private BigDecimal totalPrice;

    @NotNull(message = "订单状态不能为空")
    private Integer status; // 0未付款 1已付款 2已发货 3已完成 4已取消

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址长度不能超过200个字符")
    private String address;

    @NotBlank(message = "收货人姓名不能为空")
    @Size(min = 1, max = 50, message = "收货人姓名长度必须在1-50个字符之间")
    private String receiverName;


    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人电话格式不正确")
    private String receiverPhone;

    @NotBlank(message = "详细收货地址不能为空")
    @Size(max = 200, message = "详细收货地址长度不能超过200个字符")
    private String receiverAddress;

    @TableField(fill= FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill= FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private  LocalDateTime payTime;
}
