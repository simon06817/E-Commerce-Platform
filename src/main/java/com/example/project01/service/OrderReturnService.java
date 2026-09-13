package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.dto.ReturnApplyRequest;
import com.example.project01.entity.OrderReturn;
import com.example.project01.vo.ReturnVO;

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
