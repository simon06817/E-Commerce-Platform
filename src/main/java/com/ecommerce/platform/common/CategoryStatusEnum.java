package com.ecommerce.platform.common;

import lombok.Getter;

/** 商品分类状态枚举。 */
@Getter
public enum CategoryStatusEnum {
    DISABLED(0, "已禁用"),
    ENABLED(1, "已启用");

    private final int code;
    private final String desc;

    /** 构造商品分类状态枚举。 */
    CategoryStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 根据状态码获取枚举。 */
    public static CategoryStatusEnum fromCode(int code) {
        for (CategoryStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown category status: " + code);
    }
}
