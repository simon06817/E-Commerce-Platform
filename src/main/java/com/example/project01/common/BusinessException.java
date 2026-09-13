package com.example.project01.common;

import lombok.Getter;

/**
 * Business level exception carrying a {@link ResultCode} so the global handler
 * can return a meaningful business code instead of a generic 500.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.BAD_REQUEST;
    }
}
