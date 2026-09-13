package com.example.project01.common;

import lombok.Getter;

/**
 * Category enabled status. Values match the {@code product_category.status} column.
 */
@Getter
public enum CategoryStatusEnum {
    DISABLED(0, "disabled"),
    ENABLED(1, "enabled");

    private final int code;
    private final String desc;

    CategoryStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CategoryStatusEnum fromCode(int code) {
        for (CategoryStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown category status: " + code);
    }
}
