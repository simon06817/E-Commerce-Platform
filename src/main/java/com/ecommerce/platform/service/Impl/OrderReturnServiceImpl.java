package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.common.OrderStatusEnum;
import com.ecommerce.platform.common.ResultCode;
import com.ecommerce.platform.common.ReturnStatusEnum;
import com.ecommerce.platform.dto.ReturnApplyRequest;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.entity.OrderReturn;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.mapper.OrderReturnMapper;
import com.ecommerce.platform.service.OrderItemService;
import com.ecommerce.platform.service.OrderReturnService;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.service.ProductService;
import com.ecommerce.platform.vo.ReturnVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 退款服务实现。 */
@Service
@RequiredArgsConstructor
public class OrderReturnServiceImpl extends ServiceImpl<OrderReturnMapper, OrderReturn>
        implements OrderReturnService {

    private static final int RETURN_WINDOW_DAYS = 7;

    private final OrderItemService orderItemService;
    private final OrderService orderService;
    private final ProductService productService;

    /** 提交退款申请。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnVO applyReturn(Long buyerId, ReturnApplyRequest request) {
        OrderItem item = orderItemService.getById(request.getOrderItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Order order = orderService.getById(item.getOrderId());
        if (order == null || !buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        boolean shipped = order.getStatus() == OrderStatusEnum.SHIPPED.getCode();
        boolean completedWithinWindow = order.getStatus() == OrderStatusEnum.COMPLETED.getCode()
                && order.getCompleteTime() != null
                && !order.getCompleteTime().isBefore(
                        LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS));
        if (!shipped && !completedWithinWindow) {
            throw new BusinessException("发货后或确认收货后七天内可以申请退款");
        }
        if (lambdaQuery().eq(OrderReturn::getOrderItemId, item.getId()).count() > 0) {
            throw new BusinessException("该订单商品已经提交过退款申请");
        }
        Product product = productService.getById(item.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }

        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setOrderId(order.getId());
        orderReturn.setOrderItemId(item.getId());
        orderReturn.setBuyerId(buyerId);
        orderReturn.setSellerId(product.getSellerId());
        orderReturn.setProductId(product.getId());
        orderReturn.setQuantity(item.getQuantity());
        orderReturn.setReason(request.getReason());
        orderReturn.setStatus(ReturnStatusEnum.APPLIED.getCode());
        orderReturn.setRefundAmount(item.getSubtotal());
        orderReturn.setApplyTime(LocalDateTime.now());
        save(orderReturn);
        return toVO(orderReturn);
    }

    /** 分页查询买家退款申请。 */
    @Override
    public Page<ReturnVO> getBuyerReturns(Long buyerId, int current, int size) {
        Page<OrderReturn> page = lambdaQuery()
                .eq(OrderReturn::getBuyerId, buyerId)
                .orderByDesc(OrderReturn::getApplyTime)
                .page(new Page<>(current, size));
        return toVOPage(page);
    }

    /** 撤销退款申请。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReturn(Long buyerId, Long returnId) {
        OrderReturn orderReturn = getOwnedReturn(buyerId, null, returnId, true);
        if (orderReturn.getStatus() != ReturnStatusEnum.APPLIED.getCode()) {
            throw new BusinessException("只有待处理的退款申请可以撤销");
        }
        orderReturn.setStatus(ReturnStatusEnum.CANCELED.getCode());
        orderReturn.setHandleTime(LocalDateTime.now());
        updateById(orderReturn);
    }

    /** 分页查询卖家退款申请。 */
    @Override
    public Page<ReturnVO> getSellerReturns(Long sellerId, int current, int size) {
        Page<OrderReturn> page = lambdaQuery()
                .eq(OrderReturn::getSellerId, sellerId)
                .orderByDesc(OrderReturn::getApplyTime)
                .page(new Page<>(current, size));
        return toVOPage(page);
    }

    /** 同意退款申请并恢复库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturn(Long sellerId, Long returnId, String note) {
        OrderReturn orderReturn = getOwnedReturn(null, sellerId, returnId, false);
        // 只有待处理的退款申请可以审批。
        if (orderReturn.getStatus() != ReturnStatusEnum.APPLIED.getCode()) {
            throw new BusinessException("只有待处理的退款申请可以同意");
        }
        // 更新退款状态和处理信息。
        orderReturn.setStatus(ReturnStatusEnum.APPROVED.getCode());
        orderReturn.setHandleNote(note);
        orderReturn.setHandleTime(LocalDateTime.now());
        updateById(orderReturn);
        // 模拟退款：立即恢复库存。
        productService.increaseStock(orderReturn.getProductId(), orderReturn.getQuantity());
    }

    /** 拒绝退款申请。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReturn(Long sellerId, Long returnId, String note) {
        OrderReturn orderReturn = getOwnedReturn(null, sellerId, returnId, false);
        if (orderReturn.getStatus() != ReturnStatusEnum.APPLIED.getCode()) {
            throw new BusinessException("只有待处理的退款申请可以拒绝");
        }
        orderReturn.setStatus(ReturnStatusEnum.REJECTED.getCode());
        orderReturn.setHandleNote(note);
        orderReturn.setHandleTime(LocalDateTime.now());
        updateById(orderReturn);
    }

    /** 查询并校验退款申请归属。 */
    private OrderReturn getOwnedReturn(Long buyerId, Long sellerId, Long returnId, boolean buyerSide) {
        OrderReturn orderReturn = getById(returnId);
        if (orderReturn == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (buyerSide && !buyerId.equals(orderReturn.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!buyerSide && !sellerId.equals(orderReturn.getSellerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return orderReturn;
    }

    /** 转换分页返回对象。 */
    private Page<ReturnVO> toVOPage(Page<OrderReturn> page) {
        Page<ReturnVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    /** 转换业务返回对象。 */
    private ReturnVO toVO(OrderReturn orderReturn) {
        ReturnVO vo = new ReturnVO();
        vo.setId(orderReturn.getId());
        vo.setOrderId(orderReturn.getOrderId());
        Order order = orderService.getById(orderReturn.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setOrderItemId(orderReturn.getOrderItemId());
        vo.setProductId(orderReturn.getProductId());
        OrderItem item = orderItemService.getById(orderReturn.getOrderItemId());
        vo.setProductName(item == null ? null : item.getProductName());
        vo.setQuantity(orderReturn.getQuantity());
        vo.setReason(orderReturn.getReason());
        vo.setStatus(orderReturn.getStatus());
        vo.setStatusText(ReturnStatusEnum.fromCode(orderReturn.getStatus()).getDesc());
        vo.setRefundAmount(orderReturn.getRefundAmount());
        vo.setHandleNote(orderReturn.getHandleNote());
        vo.setApplyTime(orderReturn.getApplyTime());
        vo.setHandleTime(orderReturn.getHandleTime());
        return vo;
    }
}
