package com.ecommerce.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.common.Result;
import com.ecommerce.platform.dto.ReturnApplyRequest;
import com.ecommerce.platform.service.OrderReturnService;
import com.ecommerce.platform.vo.ReturnVO;
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

/** 退款接口。 */
@Tag(name = "Returns", description = "buyer return requests")
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('BUYER')")
public class OrderReturnController {

    private final OrderReturnService orderReturnService;

    /** 提交退款申请。 */
    @Operation(summary = "Apply for refund",
            description = "available after shipment, or within seven days after completion")
    @PostMapping
    public Result<ReturnVO> apply(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestBody @Valid ReturnApplyRequest request) {
        return Result.success(orderReturnService.applyReturn(loginUser.getId(), request));
    }

    /** 查询当前用户退款申请。 */
    @Operation(summary = "My return requests")
    @GetMapping("/my")
    public Result<Page<ReturnVO>> my(@AuthenticationPrincipal LoginUser loginUser,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderReturnService.getBuyerReturns(loginUser.getId(), page, size));
    }

    /** 撤销退款申请。 */
    @Operation(summary = "Cancel return request", description = "only before seller handles it")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id) {
        orderReturnService.cancelReturn(loginUser.getId(), id);
        return Result.success();
    }
}
