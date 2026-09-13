package com.example.project01.common;

import lombok.Getter;

/**
 * Return request lifecycle. Values match the {@code order_return.status} column.
 */
@Getter
public enum ReturnStatusEnum {
    APPLIED(0, "applied"),
    APPROVED(1, "approved and refunded"),
    REJECTED(2, "rejected"),
    CANCELED(3, "canceled");

    private final int code;
    private final String desc;

    ReturnStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReturnStatusEnum fromCode(int code) {
        for (ReturnStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown return status: " + code);
    }
}
