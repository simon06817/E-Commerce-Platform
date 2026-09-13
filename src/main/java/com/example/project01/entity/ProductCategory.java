package com.example.project01.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product category node. A parentId of 0 means a root category; children are
 * assembled only for the tree response.
 */
@Data
@TableName("product_category")
public class ProductCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "category name must not be blank")
    @Size(min = 1, max = 50, message = "category name length must be 1-50")
    private String name;

    @NotNull(message = "parent id must not be null")
    @Min(value = 0, message = "parent id cannot be negative")
    private Long parentId;

    @NotNull(message = "sort order must not be null")
    @Min(value = 0, message = "sort order cannot be negative")
    private Integer sortOrder;

    @NotNull(message = "category status must not be null")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<ProductCategory> children;
}
