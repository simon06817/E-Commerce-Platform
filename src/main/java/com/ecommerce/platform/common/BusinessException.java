package com.ecommerce.platform.common;

import lombok.Getter;

/** 业务异常。 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    /** 构造业务异常。 */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /** 构造业务异常。 */
    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.BAD_REQUEST;
    }
}
