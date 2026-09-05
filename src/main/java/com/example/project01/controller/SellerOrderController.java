package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.entity.Order;
import com.example.project01.service.OrderService;
import com.example.project01.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Seller orders", description = "seller manages orders containing own products")
@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
@Validated
public class SellerOrderController {

    private final OrderService orderService;

    @Operation(summary = "Page seller orders", description = "seller only")
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public Result<Page<Order>> list(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Integer status) {
        return Result.success(orderService.getSellerOrderPage(loginUser.getId(), page, size, status));
    }

    @Operation(summary = "Seller order detail", description = "seller only")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public Result<OrderVO> getById(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable @NotNull Long id) {
        return Result.success(orderService.getSellerOrderDetail(loginUser.getId(), id));
    }

    @Operation(summary = "Ship order", description = "seller only, paid order can be shipped")
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> ship(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable @NotNull Long id) {
        orderService.shipOrder(loginUser.getId(), id);
        return Result.success();
    }
}
