package com.example.project01.common;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    UNPAID(0, "未付款"),
    PAID(1, "已付款"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    //根据 code 获取枚举实例
    public static OrderStatusEnum fromCode(int code) {
        for (OrderStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }

}
