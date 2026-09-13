package com.example.project01.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Seller-facing product create/update payload.
 */
@Data
public class ProductRequest {

    @NotBlank(message = "product name must not be blank")
    @Size(min = 1, max = 100, message = "product name length must be 1-100")
    private String name;

    @Size(max = 500, message = "product description cannot exceed 500 chars")
    private String description;

    @NotNull(message = "product price must not be null")
    @DecimalMin(value = "0.01", message = "product price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "product stock must not be null")
    @Min(value = 0, message = "product stock cannot be negative")
    private Integer stock;

    @NotNull(message = "category id must not be null")
    private Long categoryId;

    private String mainImage;

    private Integer status;
}
