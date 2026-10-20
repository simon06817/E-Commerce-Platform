package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.dto.ReviewCreateRequest;
import com.ecommerce.platform.entity.ProductReview;
import com.ecommerce.platform.vo.ReviewSummaryVO;
import com.ecommerce.platform.vo.ReviewVO;

/** 商品评价服务。 */
public interface ProductReviewService extends IService<ProductReview> {

    /** 创建商品评价。 */
    ReviewVO createReview(Long buyerId, ReviewCreateRequest request);

    /** 分页查询商品评价。 */
    Page<ReviewVO> getProductReviews(Long productId, int current, int size);

    /** 查询商品评价汇总。 */
    ReviewSummaryVO getProductSummary(Long productId);

    /** 删除商品评价。 */
    void deleteReview(Long buyerId, Long reviewId);

    /** 分页查询卖家商品评价。 */
    Page<ReviewVO> getSellerReviews(Long sellerId, int current, int size);

    /** 回复商品评价。 */
    void replyReview(Long sellerId, Long reviewId, String content);
}
