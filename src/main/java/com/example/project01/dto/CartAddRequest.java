package com.example.project01.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for adding one product to the current buyer's cart.
 */
@Data
public class CartAddRequest {

    @NotNull(message = "product id must not be null")
    private Long productId;

    @NotNull(message = "quantity must not be null")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer num;
}
