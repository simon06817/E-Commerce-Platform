package com.example.project01.common;

import lombok.Data;
import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器错误"),

    USER_NOT_EXIST(1001, "用户不存在"),
    PRODUCT_NOT_EXIST(1002, "商品不存在"),
    STOCK_INSUFFICIENT(1003, "库存不足"),
    CATEGORY_HAS_CHILDREN(1004, "分类下存在子分类或商品，无法删除"),
    CART_ITEM_NOT_FOUND(1005, "购物车项不存在");

    private final int code;
    private final String message;
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
