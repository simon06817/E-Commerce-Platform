package com.example.project01.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.Product;
import com.example.project01.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理", description = "商品的增删改查及上下架接口")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    // 分页查询商品（支持分类、关键词、上下架状态过滤）
    @Operation(summary = "分页查询商品", description = "支持按分类、关键词、上下架状态筛选商品")
    @GetMapping
    public Result<Page<Product>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword)
                    .or()
                    .like(Product::getDescription, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return Result.success(productService.page(pageParam, wrapper));
    }

    // 商品详情
    @Operation(summary = "查询商品详情", description = "根据商品ID获取详细信息")
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable @NotNull Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error(ResultCode.PRODUCT_NOT_EXIST);
        }
        return Result.success(product);
    }

    // 新增商品
    @Operation(summary = "新增商品", description = "创建新的商品信息")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody @Valid Product product) {
        productService.save(product);
        return Result.success();
    }

    // 更新商品
    @Operation(summary = "更新商品", description = "根据ID更新商品信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable @NotNull Long id,
                               @RequestBody @Valid Product product) {
        product.setId(id);
        productService.updateById(product);
        return Result.success();
    }

    // 删除商品
    @Operation(summary = "删除商品", description = "根据ID删除商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        productService.removeById(id);
        return Result.success();
    }

    // 上下架
    @Operation(summary = "更新商品状态", description = "设置商品上架或下架状态")
    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable @NotNull Long id,
                                     @PathVariable @NotNull Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        productService.updateById(product);
        return Result.success();
    }

}
