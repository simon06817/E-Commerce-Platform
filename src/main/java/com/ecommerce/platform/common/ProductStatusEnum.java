package com.ecommerce.platform.common;

import lombok.Getter;

/** 商品状态枚举。 */
@Getter
public enum ProductStatusEnum {
    OFF_SALE(0, "已下架"),
    ON_SALE(1, "已上架");

    private final int code;
    private final String desc;

    /** 构造商品状态枚举。 */
    ProductStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 根据状态码获取枚举。 */
    public static ProductStatusEnum fromCode(int code) {
        for (ProductStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown product status: " + code);
    }
}
