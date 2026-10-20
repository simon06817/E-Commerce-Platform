package com.ecommerce.platform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 添加购物车请求对象。 */
@Data
public class CartAddRequest {

    @NotNull(message = "product id must not be null")
    private Long productId;

    @NotNull(message = "quantity must not be null")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer num;
}
