package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.dto.ReturnApplyRequest;
import com.ecommerce.platform.entity.OrderReturn;
import com.ecommerce.platform.vo.ReturnVO;

/**
 * Buyer/seller return and mock refund use cases.
 */
public interface OrderReturnService extends IService<OrderReturn> {

    ReturnVO applyReturn(Long buyerId, ReturnApplyRequest request);

    Page<ReturnVO> getBuyerReturns(Long buyerId, int current, int size);

    void cancelReturn(Long buyerId, Long returnId);

    Page<ReturnVO> getSellerReturns(Long sellerId, int current, int size);

    void approveReturn(Long sellerId, Long returnId, String note);

    void rejectReturn(Long sellerId, Long returnId, String note);
}
