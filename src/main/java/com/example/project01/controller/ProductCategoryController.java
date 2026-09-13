package com.example.project01.controller;

import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.Product;
import com.example.project01.entity.ProductCategory;
import com.example.project01.service.ProductCategoryService;
import com.example.project01.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Public category queries plus administrator category maintenance.
 */
@Tag(name = "Product categories", description = "category tree and management")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class ProductCategoryController {

    private final ProductCategoryService categoryService;
    private final ProductService productService;

    @Operation(summary = "Category tree", description = "public")
    @GetMapping("/tree")
    public Result<List<ProductCategory>> tree() {
        List<ProductCategory> list = categoryService.listEnabledCategories();
        return Result.success(buildTree(list));
    }

    @Operation(summary = "All categories", description = "public")
    @GetMapping
    public Result<List<ProductCategory>> listAll() {
        return Result.success(categoryService.listEnabledCategories());
    }

    @Operation(summary = "Category detail", description = "public")
    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable @NotNull Long id) {
        ProductCategory category = categoryService.getById(id);
        if (category == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(category);
    }

    @Operation(summary = "Create category", description = "admin only")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody @Valid ProductCategory category) {
        categoryService.addCategory(category);
        return Result.success();
    }

    @Operation(summary = "Update category", description = "admin only")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable @NotNull Long id,
                               @RequestBody @Valid ProductCategory category) {
        category.setId(id);
        categoryService.updateCategory(category);
        return Result.success();
    }

    @Operation(summary = "Update category status", description = "admin only")
    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable @NotNull Long id,
                                     @PathVariable @NotNull Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "Delete category", description = "admin only")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        long childCount = categoryService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id));
        if (childCount > 0) {
            return Result.error(ResultCode.CATEGORY_HAS_CHILDREN);
        }
        long productCount = productService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount > 0) {
            return Result.error(ResultCode.CATEGORY_HAS_CHILDREN);
        }
        categoryService.deleteCategory(id);
        return Result.success();
    }

    private List<ProductCategory> buildTree(List<ProductCategory> list) {
        Map<Long, ProductCategory> byId = list.stream()
                .collect(Collectors.toMap(ProductCategory::getId, Function.identity()));
        List<ProductCategory> roots = new ArrayList<>();
        for (ProductCategory node : list) {
            if (node.getParentId() != null && node.getParentId() != 0 && byId.containsKey(node.getParentId())) {
                ProductCategory parent = byId.get(node.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }
}
