package com.example.project01.common;

import lombok.Getter;

/**
 * Product shelf status. Values match the {@code product.status} column.
 */
@Getter
public enum ProductStatusEnum {
    OFF_SALE(0, "已下架"),
    ON_SALE(1, "已上架");

    private final int code;
    private final String desc;

    ProductStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProductStatusEnum fromCode(int code) {
        for (ProductStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown product status: " + code);
    }
}
