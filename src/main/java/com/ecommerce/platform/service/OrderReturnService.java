package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.dto.ReturnApplyRequest;
import com.ecommerce.platform.entity.OrderReturn;
import com.ecommerce.platform.vo.ReturnVO;

/** 退款服务。 */
public interface OrderReturnService extends IService<OrderReturn> {

    /** 提交退款申请。 */
    ReturnVO applyReturn(Long buyerId, ReturnApplyRequest request);

    /** 分页查询买家退款申请。 */
    Page<ReturnVO> getBuyerReturns(Long buyerId, int current, int size);

    /** 撤销退款申请。 */
    void cancelReturn(Long buyerId, Long returnId);

    /** 分页查询卖家退款申请。 */
    Page<ReturnVO> getSellerReturns(Long sellerId, int current, int size);

    /** 同意退款申请并恢复库存。 */
    void approveReturn(Long sellerId, Long returnId, String note);

    /** 拒绝退款申请。 */
    void rejectReturn(Long sellerId, Long returnId, String note);
}
