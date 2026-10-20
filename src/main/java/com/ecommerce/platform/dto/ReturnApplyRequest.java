package com.ecommerce.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 退款申请请求对象。 */
@Data
public class ReturnApplyRequest {

    @NotNull(message = "order item id must not be null")
    private Long orderItemId;

    @NotBlank(message = "return reason must not be blank")
    @Size(max = 500, message = "return reason cannot exceed 500 chars")
    private String reason;
}
