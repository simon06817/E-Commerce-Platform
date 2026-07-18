package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type= IdType.AUTO)
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 100, message = "商品名称长度必须在1-100个字符之间")
    private String name;

    @Size(max = 500, message = "商品描述不能超过500个字符")
    private String description;


    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能小于0")
    private Integer stock;


    @NotNull(message = "商品分类ID不能为空")
    private Long categoryId;

    private String mainImage;  //主页url

    @NotNull(message = "商品状态不能为空")
    private Integer status;// 0上架 1下架

    @TableField(fill= FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill= FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
