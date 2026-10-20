package com.ecommerce.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.platform.common.LoginUser;
import com.ecommerce.platform.common.Result;
import com.ecommerce.platform.dto.ReviewCreateRequest;
import com.ecommerce.platform.dto.ReviewReplyRequest;
import com.ecommerce.platform.service.ProductReviewService;
import com.ecommerce.platform.vo.ReviewSummaryVO;
import com.ecommerce.platform.vo.ReviewVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 商品评价接口。 */
@Tag(name = "Product reviews", description = "verified purchase reviews and rating summary")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ProductReviewController {

    private final ProductReviewService reviewService;

    /** 创建商品评价。 */
    @Operation(summary = "Create review", description = "buyer only, completed order required")
    @PostMapping("/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public Result<ReviewVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                   @RequestBody @Valid ReviewCreateRequest request) {
        return Result.success(reviewService.createReview(loginUser.getId(), request));
    }

    /** 删除商品评价。 */
    @Operation(summary = "Delete own review", description = "buyer only")
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable @NotNull Long id) {
        reviewService.deleteReview(loginUser.getId(), id);
        return Result.success();
    }

    /** 分页查询列表。 */
    @Operation(summary = "Product reviews", description = "public paged review list")
    @GetMapping("/products/{productId}/reviews")
    public Result<Page<ReviewVO>> list(@PathVariable @NotNull Long productId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(reviewService.getProductReviews(productId, page, size));
    }

    /** 查询商品评价汇总。 */
    @Operation(summary = "Product rating summary", description = "public average rating")
    @GetMapping("/products/{productId}/reviews/summary")
    public Result<ReviewSummaryVO> summary(@PathVariable @NotNull Long productId) {
        return Result.success(reviewService.getProductSummary(productId));
    }

    /** 分页查询卖家评价。 */
    @Operation(summary = "Seller review list", description = "seller only")
    @GetMapping("/seller/reviews")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Page<ReviewVO>> sellerReviews(@AuthenticationPrincipal LoginUser loginUser,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.success(reviewService.getSellerReviews(loginUser.getId(), page, size));
    }

    /** 回复商品评价。 */
    @Operation(summary = "Reply to review", description = "seller only, own product")
    @PutMapping("/seller/reviews/{id}/reply")
    @PreAuthorize("hasRole('SELLER')")
    public Result<Void> reply(@AuthenticationPrincipal LoginUser loginUser,
                              @PathVariable @NotNull Long id,
                              @RequestBody @Valid ReviewReplyRequest request) {
        reviewService.replyReview(loginUser.getId(), id, request.getContent());
        return Result.success();
    }
}
