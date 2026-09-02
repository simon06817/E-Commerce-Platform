package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.dto.OrderCreateRequest;
import com.example.project01.entity.Order;
import com.example.project01.service.OrderService;
import com.example.project01.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "buyer order management")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Page my orders", description = "buyer only")
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public Result<Page<Order>> list(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Integer status) {
        return Result.success(orderService.getOrderPage(loginUser.getId(), page, size, status));
    }

    @Operation(summary = "Order detail", description = "buyer only")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public Result<OrderVO> getById(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable @NotNull Long id) {
        return Result.success(orderService.getOrderDetail(loginUser.getId(), id));
    }

    @Operation(summary = "Create order from cart", description = "buyer only")
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public Result<OrderVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestBody @Valid OrderCreateRequest request) {
        Order order = orderService.createOrder(loginUser.getId(), request);
        return Result.success(orderService.getOrderDetail(loginUser.getId(), order.getId()));
    }

    @Operation(summary = "Cancel order", description = "buyer only")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id) {
        orderService.cancelOrder(loginUser.getId(), id);
        return Result.success();
    }

    @Operation(summary = "Pay order", description = "buyer only")
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> pay(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable @NotNull Long id) {
        orderService.payOrder(loginUser.getId(), id);
        return Result.success();
    }
}
