package com.example.project01.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "resource not found"),
    INTERNAL_SERVER_ERROR(500, "internal server error"),

    USER_NOT_EXIST(1001, "user does not exist"),
    PRODUCT_NOT_EXIST(1002, "product does not exist"),
    STOCK_INSUFFICIENT(1003, "insufficient stock"),
    CATEGORY_HAS_CHILDREN(1004, "category has children or products, cannot delete"),
    CART_ITEM_NOT_FOUND(1005, "cart item does not exist"),
    USERNAME_OR_PASSWORD_ERROR(1006, "username or password is wrong"),
    USER_ALREADY_EXISTS(1007, "username already exists"),
    PRODUCT_NOT_OWNED(1008, "product is not owned by current seller"),
    INVALID_ROLE(1009, "invalid role");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
