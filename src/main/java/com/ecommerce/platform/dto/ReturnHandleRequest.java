package com.ecommerce.platform.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 退款处理请求对象。 */
@Data
public class ReturnHandleRequest {

    @Size(max = 500, message = "handle note cannot exceed 500 chars")
    private String note;
}
