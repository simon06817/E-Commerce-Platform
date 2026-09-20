package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.project01.common.BusinessException;
import com.example.project01.common.Result;
import com.example.project01.entity.Cart;
import com.example.project01.entity.Order;
import com.example.project01.entity.Product;
import com.example.project01.entity.UserBuyer;
import com.example.project01.entity.UserSeller;
import com.example.project01.service.CartService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.service.UserBuyerService;
import com.example.project01.service.UserSellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator endpoints for viewing and removing buyer/seller accounts.
 */
@Tag(name = "Admin users", description = "admin manages buyers and sellers")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserBuyerService buyerService;
    private final UserSellerService sellerService;
    private final OrderService orderService;
    private final CartService cartService;
    private final ProductService productService;

    @Operation(summary = "List buyers")
    @GetMapping("/buyers")
    public Result<Page<UserBuyer>> buyers(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(buyerService.pageWithKeyword(page, size, keyword));
    }

    @Operation(summary = "List sellers")
    @GetMapping("/sellers")
    public Result<Page<UserSeller>> sellers(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(sellerService.pageWithKeyword(page, size, keyword));
    }

    @Operation(summary = "Delete buyer")
    @DeleteMapping("/buyers/{id}")
    public Result<Void> deleteBuyer(@PathVariable Long id) {
        long orderCount = orderService.count(
                new LambdaQueryWrapper<Order>().eq(Order::getBuyerId, id));
        if (orderCount > 0) {
            throw new BusinessException("该买家存在历史订单，不能直接删除");
        }
        long cartCount = cartService.count(
                new LambdaQueryWrapper<Cart>().eq(Cart::getBuyerId, id));
        if (cartCount > 0) {
            throw new BusinessException("该买家购物车尚未清空，不能直接删除");
        }
        buyerService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "Delete seller")
    @DeleteMapping("/sellers/{id}")
    public Result<Void> deleteSeller(@PathVariable Long id) {
        long productCount = productService.count(
                new LambdaQueryWrapper<Product>().eq(Product::getSellerId, id));
        if (productCount > 0) {
            throw new BusinessException("该卖家仍有商品，不能直接删除");
        }
        sellerService.removeById(id);
        return Result.success();
    }
}
