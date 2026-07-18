package com.example.project01.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.Order;
import com.example.project01.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "订单管理", description = "订单的创建、查询、支付、取消等接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    // 分页查询订单（可按用户、状态筛选）
    @Operation(summary = "分页查询订单", description = "支持按用户ID、订单状态筛选订单列表")
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Page<Order>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Long userId,
                                    @RequestParam(required = false) Integer status) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderService.page(pageParam, wrapper));
    }

    // 订单详情
    @Operation(summary = "查询订单详情", description = "根据订单ID获取订单详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Order> getById(@PathVariable @NotNull Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(order);
    }

    // 创建订单（简化：从购物车生成订单，此处直接接收订单对象）
    @Operation(summary = "创建订单", description = "提交新订单，初始状态为待支付")
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Order> create(@RequestBody @Valid Order order) {
        // 实际业务中需校验库存、计算总价等，此处简化直接保存
        order.setStatus(0); // 0-待支付
        order.setTotalPrice(BigDecimal.ZERO); // 应通过商品计算
        orderService.save(order);
        // 可调用库存扣减服务
        return Result.success(order);
    }

    // 取消订单
    @Operation(summary = "取消订单", description = "取消待支付状态的订单")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(@PathVariable @NotNull Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            return Result.error(ResultCode.BAD_REQUEST.getCode(), "只有待支付订单可取消");
        }
        order.setStatus(2); // 2-已取消
        orderService.updateById(order);
        // 恢复库存
        return Result.success();
    }

    // 支付订单（模拟）
    @Operation(summary = "支付订单", description = "模拟支付操作，将订单状态改为已支付")
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> pay(@PathVariable @NotNull Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            return Result.error(ResultCode.BAD_REQUEST.getCode(), "订单状态异常");
        }
        order.setStatus(1); // 1-已支付
        orderService.updateById(order);
        return Result.success();
    }

    // 删除订单
    @Operation(summary = "删除订单", description = "根据ID删除订单记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        orderService.removeById(id);
        return Result.success();
    }

}
