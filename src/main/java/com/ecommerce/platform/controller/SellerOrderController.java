package com.ecommerce.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.common.Result;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.vo.OrderVO;
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

/** 卖家订单接口。 */
@Tag(name = "Seller orders", description = "seller manages orders containing own products")
@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
@Validated
public class SellerOrderController {

    private final OrderService orderService;

    /** 分页查询列表。 */
    @Operation(summary = "Page seller orders", description = "seller only")
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public Result<Page<Order>> list(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Integer status) {
        return Result.success(orderService.getSellerOrderPage(loginUser.getId(), page, size, status));
    }

    /** 分页查询订单及明细。 */
    @Operation(summary = "Page seller order details in one request", description = "seller only")
    @GetMapping("/page-details")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Page<OrderVO>> pageDetails(@AuthenticationPrincipal LoginUser loginUser,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(orderService.getSellerOrderPageDetails(
                loginUser.getId(), page, size, status));
    }

    /** 根据主键查询数据。 */
    @Operation(summary = "Seller order detail", description = "seller only")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public Result<OrderVO> getById(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable @NotNull Long id) {
        return Result.success(orderService.getSellerOrderDetail(loginUser.getId(), id));
    }

    /** 订单发货。 */
    @Operation(summary = "Ship order", description = "seller only, paid order can be shipped")
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> ship(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable @NotNull Long id) {
        orderService.shipOrder(loginUser.getId(), id);
        return Result.success();
    }

}
