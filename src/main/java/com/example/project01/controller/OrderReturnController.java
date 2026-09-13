package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.dto.ReturnApplyRequest;
import com.example.project01.service.OrderReturnService;
import com.example.project01.vo.ReturnVO;
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

/**
 * Buyer return/refund endpoints.
 */
@Tag(name = "Returns", description = "buyer return requests")
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('BUYER')")
public class OrderReturnController {

    private final OrderReturnService orderReturnService;

    @Operation(summary = "Apply for return", description = "completed order and seven-day window required")
    @PostMapping
    public Result<ReturnVO> apply(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestBody @Valid ReturnApplyRequest request) {
        return Result.success(orderReturnService.applyReturn(loginUser.getId(), request));
    }

    @Operation(summary = "My return requests")
    @GetMapping("/my")
    public Result<Page<ReturnVO>> my(@AuthenticationPrincipal LoginUser loginUser,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderReturnService.getBuyerReturns(loginUser.getId(), page, size));
    }

    @Operation(summary = "Cancel return request", description = "only before seller handles it")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id) {
        orderReturnService.cancelReturn(loginUser.getId(), id);
        return Result.success();
    }
}
