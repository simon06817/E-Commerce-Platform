package com.example.project01.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.Cart;
import com.example.project01.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车管理", description = "购物车的增删改查接口")
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    // 获取当前用户购物车列表（此处userId从请求头或token获取，简化处理传参）
    @Operation(summary = "查询购物车列表", description = "获取指定用户的购物车商品列表")
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<List<Cart>> listByUser(@RequestParam @NotNull Long userId) {
        List<Cart> list = cartService.list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
        return Result.success(list);
    }

    // 加入购物车
    @Operation(summary = "加入购物车", description = "将商品添加到购物车")
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Void> add(@RequestBody @Valid Cart cart) {
        // 检查是否已存在相同商品，存在则更新数量（业务可自行扩展）
        cartService.save(cart);
        return Result.success();
    }

    // 更新购物车项数量
    @Operation(summary = "更新购物车商品数量", description = "修改购物车中指定商品的数量")
    @PutMapping("/{id}/num")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Void> updateQuantity(@PathVariable @NotNull Long id,
                                       @RequestParam @NotNull Integer num) {
        Cart cart = cartService.getById(id);
        if (cart == null) {
            return Result.error(ResultCode.CART_ITEM_NOT_FOUND);
        }
        cart.setNum(num);
        cartService.updateById(cart);
        return Result.success();
    }

    // 删除购物车项
    @Operation(summary = "删除购物车商品", description = "从购物车中移除指定商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @NotNull Long id) {
        cartService.removeById(id);
        return Result.success();
    }

    // 清空用户购物车
    @Operation(summary = "清空购物车", description = "清空指定用户的所有购物车商品")
    @DeleteMapping("/clear/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Void> clear(@PathVariable @NotNull Long userId) {
        cartService.remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return Result.success();
    }

}
