package com.example.project01.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.LoginUser;
import com.example.project01.common.Result;
import com.example.project01.entity.OrderNotification;
import com.example.project01.service.OrderNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification endpoints for buyers, sellers and admins.
 */
@Tag(name = "Notifications", description = "order event notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final OrderNotificationService notificationService;

    @Operation(summary = "My notifications", description = "current authenticated user")
    @GetMapping("/my")
    public Result<Page<OrderNotification>> my(@AuthenticationPrincipal LoginUser loginUser,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(notificationService.getMyNotifications(
                loginUser.getRole().name(), loginUser.getId(), page, size));
    }

    @Operation(summary = "Mark notification as read")
    @PutMapping("/{id}/read")
    public Result<Void> read(@AuthenticationPrincipal LoginUser loginUser,
                             @PathVariable @NotNull Long id) {
        notificationService.markRead(loginUser.getRole().name(), loginUser.getId(), id);
        return Result.success();
    }
}
