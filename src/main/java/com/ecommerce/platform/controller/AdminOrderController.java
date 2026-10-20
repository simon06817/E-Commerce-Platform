package com.ecommerce.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.platform.common.Result;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端订单接口。 */
@Tag(name = "Admin orders", description = "admin order query and force cancellation")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /** 分页查询列表。 */
    @Operation(summary = "Page all orders")
    @GetMapping
    public Result<Page<Order>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String orderNo,
                                    @RequestParam(required = false) Long buyerId,
                                    @RequestParam(required = false) Integer status) {
        return Result.success(orderService.getAdminOrderPage(page, size, orderNo, buyerId, status));
    }

    /** 根据主键查询数据。 */
    @Operation(summary = "Admin order detail")
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable @NotNull Long id) {
        return Result.success(orderService.getAdminOrderDetail(id));
    }

    /** 强制关闭订单。 */
    @Operation(summary = "Force cancel order", description = "unpaid/paid/shipped orders only")
    @PutMapping("/{id}/force-cancel")
    public Result<Void> forceCancel(@PathVariable @NotNull Long id) {
        orderService.forceCancelOrder(id);
        return Result.success();
    }
}
