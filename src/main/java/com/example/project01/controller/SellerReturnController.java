package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.dto.ReturnHandleRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller return handling endpoints.
 */
@Tag(name = "Seller returns", description = "seller approves or rejects return requests")
@RestController
@RequestMapping("/api/seller/returns")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('SELLER')")
public class SellerReturnController {

    private final OrderReturnService orderReturnService;

    @Operation(summary = "Seller return list")
    @GetMapping
    public Result<Page<ReturnVO>> list(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderReturnService.getSellerReturns(loginUser.getId(), page, size));
    }

    @Operation(summary = "Approve return", description = "restores stock and performs mock refund")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@AuthenticationPrincipal LoginUser loginUser,
                                @PathVariable @NotNull Long id,
                                @RequestBody(required = false) @Valid ReturnHandleRequest request) {
        orderReturnService.approveReturn(loginUser.getId(), id,
                request == null ? null : request.getNote());
        return Result.success();
    }

    @Operation(summary = "Reject return")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id,
                               @RequestBody(required = false) @Valid ReturnHandleRequest request) {
        orderReturnService.rejectReturn(loginUser.getId(), id,
                request == null ? null : request.getNote());
        return Result.success();
    }
}
