package com.example.project01.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.project01.common.Result;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.User;
import com.example.project01.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@Tag(name = "用户管理", description = "用户的增删改查接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    //分页查询用户
    @Operation(summary = "分页查询用户列表", description = "支持按用户名、邮箱、手机号关键词搜索")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<User>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String keyword) {
        Page<User> pageParm = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword)

                    .or()
                    .like(User::getPhone, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return Result.success(userService.page(pageParm, wrapper));
    }

    //根据ID查询用户
    @Operation(summary = "根据ID查询用户", description = "根据用户ID获取详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<User> getById(@PathVariable @NotNull Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        return Result.success(user);
    }

    //新增用户
    @Operation(summary = "新增用户", description = "创建新用户")
    @PostMapping
    public Result<Void> save(@RequestBody @Valid User user){
        userService.save(user);
        return Result.success();
    }

    //更新用户
    @Operation(summary = "更新用户", description = "根据ID更新用户信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public Result<Void> update(@PathVariable @NotNull Long id,
                               @RequestBody @Valid User user){
        user.setId(id);
        userService.updateUser(user);
        return Result.success();
    }

    //删除用户
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable @NotNull Long id){
        userService.removeById(id);
        return Result.success();
    }

}