package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.dto.ReviewCreateRequest;
import com.example.project01.entity.ProductReview;
import com.example.project01.vo.ReviewSummaryVO;
import com.example.project01.vo.ReviewVO;

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
