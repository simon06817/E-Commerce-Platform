package com.example.project01.common;

import lombok.Getter;

/**
 * Outbox delivery status. Values match the {@code order_outbox.status} column.
 */
@Getter
public enum OutboxStatusEnum {
    PENDING(0, "pending"),
    SENT(1, "sent");

    private final int code;
    private final String desc;

    OutboxStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OutboxStatusEnum fromCode(int code) {
        for (OutboxStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown outbox status: " + code);
    }
}
