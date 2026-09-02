package com.example.project01.controller;

import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.dto.CartAddRequest;
import com.example.project01.dto.CartUpdateRequest;
import com.example.project01.entity.Cart;
import com.example.project01.service.CartService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cart", description = "buyer cart management")
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @Operation(summary = "My cart", description = "buyer only")
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public Result<List<Cart>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(cartService.getCartList(loginUser.getId()));
    }

    @Operation(summary = "Add to cart", description = "buyer only")
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestBody @Valid CartAddRequest request) {
        cartService.addCart(loginUser.getId(), request.getProductId(), request.getNum());
        return Result.success();
    }

    @Operation(summary = "Update cart quantity", description = "buyer only")
    @PutMapping("/{id}/num")
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> updateNum(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable @NotNull Long id,
                                  @RequestBody @Valid CartUpdateRequest request) {
        cartService.updateCartNum(loginUser.getId(), id, request.getNum());
        return Result.success();
    }

    @Operation(summary = "Delete cart item", description = "buyer only")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id) {
        cartService.deleteCart(loginUser.getId(), id);
        return Result.success();
    }

    @Operation(summary = "Clear cart", description = "buyer only")
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> clear(@AuthenticationPrincipal LoginUser loginUser) {
        cartService.clearCart(loginUser.getId());
        return Result.success();
    }
}
