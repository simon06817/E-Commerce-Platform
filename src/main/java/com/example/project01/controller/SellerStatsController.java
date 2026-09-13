package com.example.project01.controller;

import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.service.OrderService;
import com.example.project01.vo.SellerStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller revenue statistics endpoints.
 */
@Tag(name = "Seller stats", description = "seller revenue statistics")
@RestController
@RequestMapping("/api/seller/stats")
@RequiredArgsConstructor
public class SellerStatsController {

    private final OrderService orderService;

    @Operation(summary = "Seller revenue statistics", description = "today/7d/30d/all")
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public Result<SellerStatsVO> stats(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestParam(defaultValue = "30d") String range) {
        return Result.success(orderService.getSellerStats(loginUser.getId(), range));
    }
}
