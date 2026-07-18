package com.example.project01.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品分类管理", description = "商品分类的增删改查接口")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class ProductCategoryController {

    private final ProductCategoryService categoryService;
    private final ProductService productService;

    // 获取分类树（层级结构）
    @Operation(summary = "获取分类树", description = "获取所有商品分类的树形结构数据")
    @GetMapping("/tree")
    public Result<List<ProductCategory>> tree() {
        List<ProductCategory> list = categoryService.list(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSortOrder)
        );
        // 构建树形结构（此处简化为返回所有，实际可递归构建）
        return Result.success(list);
    }

    // 获取所有分类（平铺）
    @Operation(summary = "获取所有分类", description = "获取所有商品分类的平铺列表")
    @GetMapping
    public Result<List<ProductCategory>> listAll() {
        return Result.success(categoryService.list());
    }

    // 根据ID查询分类
    @Operation(summary = "根据ID查询分类", description = "根据分类ID获取详细信息")
    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable @NotNull Long id) {
        ProductCategory category = categoryService.getById(id);
        if (category == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(category);
    }

    // 新增分类
    @Operation(summary = "新增分类", description = "创建新的商品分类")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody @Valid ProductCategory category) {
        categoryService.save(category);
        return Result.success();
    }

    // 更新分类
    @Operation(summary = "更新分类", description = "根据ID更新分类信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable @NotNull Long id,
                               @RequestBody @Valid ProductCategory category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.success();
    }

    // 删除分类（需检查是否有子分类或关联商品）
    @Operation(summary = "删除分类", description = "根据ID删除分类，需确保无子分类和关联商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        // 检查是否存在子分类
        long childCount = categoryService.count(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id));
        if (childCount > 0) {
            return Result.error(ResultCode.CATEGORY_HAS_CHILDREN);
        }
        // 检查是否有关联商品
        long productCount = productService.count(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount > 0) {
            return Result.error(ResultCode.CATEGORY_HAS_CHILDREN);
        }
        categoryService.removeById(id);
        return Result.success();
    }

}
