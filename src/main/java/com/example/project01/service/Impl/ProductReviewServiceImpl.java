package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.OrderStatusEnum;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.ReviewCreateRequest;
import com.example.project01.entity.Order;
import com.example.project01.entity.OrderItem;
import com.example.project01.entity.OrderReturn;
import com.example.project01.entity.ProductReview;
import com.example.project01.entity.UserBuyer;
import com.example.project01.mapper.ProductReviewMapper;
import com.example.project01.mapper.OrderReturnMapper;
import com.example.project01.service.OrderItemService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.service.ProductReviewService;
import com.example.project01.service.UserBuyerService;
import com.example.project01.vo.ReviewSummaryVO;
import com.example.project01.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Review service enforcing verified-purchase rules and masking buyer identity
 * in public responses.
 */
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview>
        implements ProductReviewService {

    private final OrderItemService orderItemService;
    private final OrderService orderService;
    private final UserBuyerService userBuyerService;
    private final ProductService productService;
    private final OrderReturnMapper orderReturnMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO createReview(Long buyerId, ReviewCreateRequest request) {
        OrderItem item = orderItemService.getById(request.getOrderItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Order order = orderService.getById(item.getOrderId());
        if (order == null || !buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // Reviews are only allowed after the buyer confirmed receipt.
        if (order.getStatus() != OrderStatusEnum.COMPLETED.getCode()) {
            throw new BusinessException(ResultCode.REVIEW_NOT_ALLOWED);
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException("评价内容不能为空");
        }
        long refundedCount = orderReturnMapper.selectCount(
                new QueryWrapper<OrderReturn>()
                        .eq("order_item_id", item.getId())
                        .eq("status", com.example.project01.common.ReturnStatusEnum
                                .APPROVED.getCode()));
        if (refundedCount > 0) {
            throw new BusinessException(ResultCode.REFUNDED_PRODUCT_CANNOT_REVIEW);
        }
        if (lambdaQuery().eq(ProductReview::getOrderItemId, item.getId()).count() > 0) {
            throw new BusinessException(ResultCode.REVIEW_ALREADY_EXISTS);
        }

        ProductReview review = new ProductReview();
        review.setProductId(item.getProductId());
        review.setBuyerId(buyerId);
        review.setOrderId(order.getId());
        review.setOrderItemId(item.getId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        save(review);
        return toVO(review);
    }

    @Override
    public Page<ReviewVO> getProductReviews(Long productId, int current, int size) {
        Page<ProductReview> page = lambdaQuery()
                .eq(ProductReview::getProductId, productId)
                .orderByDesc(ProductReview::getCreateTime)
                .page(new Page<>(current, size));

        Page<ReviewVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public ReviewSummaryVO getProductSummary(Long productId) {
        QueryWrapper<ProductReview> wrapper = new QueryWrapper<>();
        wrapper.select("COALESCE(AVG(rating), 0) AS average_rating", "COUNT(*) AS review_count")
                .eq("product_id", productId);
        List<Map<String, Object>> rows = baseMapper.selectMaps(wrapper);
        if (rows.isEmpty()) {
            return new ReviewSummaryVO(0.0, 0L);
        }
        Map<String, Object> row = rows.get(0);
        double average = row.get("average_rating") == null
                ? 0.0 : ((Number) row.get("average_rating")).doubleValue();
        long count = row.get("review_count") == null
                ? 0L : ((Number) row.get("review_count")).longValue();
        return new ReviewSummaryVO(Math.round(average * 10.0) / 10.0, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long buyerId, Long reviewId) {
        ProductReview review = getById(reviewId);
        if (review == null || !buyerId.equals(review.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        removeById(reviewId);
    }

    @Override
    public Page<ReviewVO> getSellerReviews(Long sellerId, int current, int size) {
        List<Long> productIds = productService.lambdaQuery()
                .eq(com.example.project01.entity.Product::getSellerId, sellerId)
                .list()
                .stream()
                .map(com.example.project01.entity.Product::getId)
                .toList();
        if (productIds.isEmpty()) {
            return new Page<>(current, size);
        }
        Page<ProductReview> page = lambdaQuery()
                .in(ProductReview::getProductId, productIds)
                .orderByDesc(ProductReview::getCreateTime)
                .page(new Page<>(current, size));
        Page<ReviewVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(Long sellerId, Long reviewId, String content) {
        ProductReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        com.example.project01.entity.Product product = productService.getById(review.getProductId());
        if (product == null || !sellerId.equals(product.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        review.setReplyContent(content);
        review.setReplyTime(java.time.LocalDateTime.now());
        updateById(review);
    }

    private ReviewVO toVO(ProductReview review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setProductId(review.getProductId());
        com.example.project01.entity.Product product = productService.getById(review.getProductId());
        vo.setProductName(product == null ? null : product.getName());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setReplyContent(review.getReplyContent());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        vo.setBuyerName(maskBuyerName(review.getBuyerId()));
        return vo;
    }

    private String maskBuyerName(Long buyerId) {
        UserBuyer buyer = userBuyerService.getById(buyerId);
        String username = buyer == null ? "anonymous" : buyer.getUsername();
        if (username.length() <= 2) {
            return username.charAt(0) + "***";
        }
        return username.substring(0, 2) + "***" + username.substring(username.length() - 2);
    }
}
