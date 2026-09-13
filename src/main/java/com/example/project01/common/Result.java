package com.example.project01.common;

import lombok.Data;

/**
 * Unified HTTP response envelope: code, message and payload.
 *
 * @param <T> payload type
 */
@Data
public class Result<T> {
    private int code;

    private String message;

    private T data;

    private Result(){}

    private Result(int code,String message,T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data){
        return new Result<>(ResultCode.SUCCESS.getCode(),ResultCode.SUCCESS.getMessage(),data);
    }

    public static <T> Result<T> success(String message,T data){
        return new Result<>(ResultCode.SUCCESS.getCode(),message,data);
    }

    public static  Result<Void> success(){
        return new Result<>(ResultCode.SUCCESS.getCode(),ResultCode.SUCCESS.getMessage(),null);
    }

    public static  <T> Result<T> error(ResultCode resultCode){
        return new Result<>(resultCode.getCode(),resultCode.getMessage(),null);
    }

    public static <T> Result<T> error(int code,String message){
        return new Result<>(code,message,null);
    }

}
