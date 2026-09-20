package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.Result;
import com.example.project01.entity.ProductCategory;
import com.example.project01.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator category list, including disabled categories.
 */
@Tag(name = "Admin categories", description = "admin views all category states")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final ProductCategoryService categoryService;

    @Operation(summary = "Page all categories")
    @GetMapping
    public Result<Page<ProductCategory>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.success(categoryService.getCategoryPage(page, size, name, status));
    }
}
