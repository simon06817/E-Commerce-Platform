package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.dto.ReviewCreateRequest;
import com.ecommerce.platform.entity.ProductReview;
import com.ecommerce.platform.vo.ReviewSummaryVO;
import com.ecommerce.platform.vo.ReviewVO;

/**
 * Product review use cases: verified purchase review, public listing and summary.
 */
public interface ProductReviewService extends IService<ProductReview> {

    ReviewVO createReview(Long buyerId, ReviewCreateRequest request);

    Page<ReviewVO> getProductReviews(Long productId, int current, int size);

    ReviewSummaryVO getProductSummary(Long productId);

    void deleteReview(Long buyerId, Long reviewId);

    Page<ReviewVO> getSellerReviews(Long sellerId, int current, int size);

    void replyReview(Long sellerId, Long reviewId, String content);
}
