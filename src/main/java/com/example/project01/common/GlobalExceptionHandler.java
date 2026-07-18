package com.example.project01.common;


import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



@RestControllerAdvice
public class GlobalExceptionHandler {

    // 参数校验异常
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e){
        String msg=e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(ResultCode.BAD_REQUEST.getCode(),msg);
    }

    // 运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> HandleRuntimeException(RuntimeException e){
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(),e.getMessage());
    }

    // 其他异常
    @ExceptionHandler(Exception.class)
    public Result<Void> HandleException(Exception e){
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR);
    }

}
