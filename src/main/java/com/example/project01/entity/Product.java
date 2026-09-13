package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product aggregate owned by a seller. Maps to the {@code product} table.
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "seller id must not be null")
    private Long sellerId;

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

    @NotNull(message = "product status must not be null")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
