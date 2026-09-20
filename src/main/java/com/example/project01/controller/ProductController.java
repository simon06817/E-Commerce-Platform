package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.ProductRequest;
import com.example.project01.entity.Product;
import com.example.project01.service.ProductService;
import com.example.project01.vo.ProductRecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.List;

/**
 * Public product browsing and seller-owned product management endpoints.
 */
@Tag(name = "Products", description = "product browsing and seller management")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Page products", description = "public")
    @GetMapping
    public Result<Page<Product>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String shopName,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(
                productService.getProductPage(page, size, categoryId, keyword, shopName, status));
    }

    @Operation(summary = "Recommend products",
            description = "public, ranked by paid sales and review rating")
    @GetMapping("/recommendations")
    public Result<List<ProductRecommendationVO>> recommendations(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "5") int limit) {
        return Result.success(productService.getRecommendations(categoryId, limit));
    }

    @Operation(summary = "Product detail", description = "public")
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable @NotNull Long id) {
        Product product = productService.getProductDetail(id);
        if (product == null) {
            return Result.error(ResultCode.PRODUCT_NOT_EXIST);
        }
        return Result.success(product);
    }

    @Operation(summary = "Create product", description = "seller only")
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> save(@AuthenticationPrincipal LoginUser loginUser,
                             @RequestBody @Valid ProductRequest request) {
        productService.createProduct(loginUser.getId(), request);
        return Result.success();
    }

    @Operation(summary = "Update product", description = "seller only, own products")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id,
                               @RequestBody @Valid ProductRequest request) {
        productService.updateProduct(loginUser.getId(), id, request);
        return Result.success();
    }

    @Operation(summary = "Update product status", description = "seller only, own products")
    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> updateStatus(@AuthenticationPrincipal LoginUser loginUser,
                                     @PathVariable @NotNull Long id,
                                     @PathVariable @NotNull Integer status) {
        productService.updateProductStatus(loginUser.getId(), id, status);
        return Result.success();
    }

    @Operation(summary = "Delete product", description = "seller only, own products")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id) {
        productService.deleteProduct(loginUser.getId(), id);
        return Result.success();
    }
}
