package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.entity.Product;
import com.example.project01.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller-owned product queries. Product mutations remain under /api/products
 * and are ownership-checked by ProductService.
 */
@Tag(name = "Seller products", description = "seller-owned product management")
@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {

    private final ProductService productService;

    @Operation(summary = "Page my products", description = "seller only")
    @GetMapping
    public Result<Page<Product>> list(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(productService.getSellerProductPage(
                loginUser.getId(), page, size, categoryId, keyword, status));
    }
}
