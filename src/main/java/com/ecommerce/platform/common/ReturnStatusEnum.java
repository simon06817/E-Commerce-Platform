package com.ecommerce.platform.common;

import lombok.Getter;

/** 退款状态枚举。 */
@Getter
public enum ReturnStatusEnum {
    APPLIED(0, "待卖家处理"),
    APPROVED(1, "已同意并退款"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已撤销");

    private final int code;
    private final String desc;

    /** 构造退款状态枚举。 */
    ReturnStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 根据状态码获取枚举。 */
    public static ReturnStatusEnum fromCode(int code) {
        for (ReturnStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown return status: " + code);
    }
}
