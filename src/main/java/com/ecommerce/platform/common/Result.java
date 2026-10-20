package com.ecommerce.platform.common;

import lombok.Data;

/** 统一响应对象。 */
@Data
public class Result<T> {
    private int code;

    private String message;

    private T data;

    /** 构造统一响应对象。 */
    private Result(){}

    /** 构造统一响应对象。 */
    private Result(int code,String message,T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 构建成功响应。 */
    public static <T> Result<T> success(T data){
        return new Result<>(ResultCode.SUCCESS.getCode(),ResultCode.SUCCESS.getMessage(),data);
    }

    /** 构建成功响应。 */
    public static <T> Result<T> success(String message,T data){
        return new Result<>(ResultCode.SUCCESS.getCode(),message,data);
    }

    /** 构建成功响应。 */
    public static  Result<Void> success(){
        return new Result<>(ResultCode.SUCCESS.getCode(),ResultCode.SUCCESS.getMessage(),null);
    }

    /** 构建失败响应。 */
    public static  <T> Result<T> error(ResultCode resultCode){
        return new Result<>(resultCode.getCode(),resultCode.getMessage(),null);
    }

    /** 构建失败响应。 */
    public static <T> Result<T> error(int code,String message){
        return new Result<>(code,message,null);
    }

}
