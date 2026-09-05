package com.example.project01.vo;

import com.example.project01.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long buyerId;

    private BigDecimal totalAmount;

    private Integer status;

    private String statusText;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private List<OrderItem> items;
}
