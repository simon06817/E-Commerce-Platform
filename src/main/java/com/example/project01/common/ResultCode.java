package com.example.project01.common;

import lombok.Getter;

/**
 * Central catalogue of business and HTTP result codes.
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),
    FORBIDDEN(403, "没有权限执行此操作"),
    NOT_FOUND(404, "请求的资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误，请稍后重试"),
    TOO_MANY_REQUESTS(429, "操作过于频繁，请稍后再试"),

    USER_NOT_EXIST(1001, "用户不存在"),
    PRODUCT_NOT_EXIST(1002, "商品不存在"),
    STOCK_INSUFFICIENT(1003, "商品库存不足"),
    CATEGORY_HAS_CHILDREN(1004, "分类下存在子分类或商品，无法删除"),
    CART_ITEM_NOT_FOUND(1005, "购物车商品不存在"),
    USERNAME_OR_PASSWORD_ERROR(1006, "用户名或密码错误"),
    USER_ALREADY_EXISTS(1007, "用户名已存在"),
    PRODUCT_NOT_OWNED(1008, "该商品不属于当前卖家"),
    INVALID_ROLE(1009, "用户角色无效"),
    REVIEW_NOT_ALLOWED(1010, "确认收货后才能发布评价"),
    REVIEW_ALREADY_EXISTS(1011, "该订单商品已经评价过了"),
    REFUNDED_PRODUCT_CANNOT_REVIEW(1012, "已退款商品不能评价"),
    BUYER_PROFILE_INCOMPLETE(1013, "请先补全个人信息后再下单");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
