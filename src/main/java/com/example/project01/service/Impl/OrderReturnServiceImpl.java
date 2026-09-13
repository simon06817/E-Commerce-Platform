package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.OrderStatusEnum;
import com.example.project01.common.ResultCode;
import com.example.project01.common.ReturnStatusEnum;
import com.example.project01.dto.ReturnApplyRequest;
import com.example.project01.entity.Order;
import com.example.project01.entity.OrderItem;
import com.example.project01.entity.OrderReturn;
import com.example.project01.entity.Product;
import com.example.project01.mapper.OrderReturnMapper;
import com.example.project01.service.OrderItemService;
import com.example.project01.service.OrderReturnService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.vo.ReturnVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Return service. Approval performs a mock refund and restores stock because
 * the project has no real payment gateway.
 */
@Service
@RequiredArgsConstructor
public class OrderReturnServiceImpl extends ServiceImpl<OrderReturnMapper, OrderReturn>
        implements OrderReturnService {

    private static final int RETURN_WINDOW_DAYS = 7;

    private final OrderItemService orderItemService;
    private final OrderService orderService;
    private final ProductService productService;

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
        if (order.getStatus() != OrderStatusEnum.COMPLETED.getCode()) {
            throw new BusinessException("only completed orders can be returned");
        }
        if (order.getCompleteTime() == null
                || order.getCompleteTime().isBefore(LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS))) {
            throw new BusinessException("return window has expired");
        }
        if (lambdaQuery().eq(OrderReturn::getOrderItemId, item.getId()).count() > 0) {
            throw new BusinessException("return request already exists");
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

    @Override
    public Page<ReturnVO> getBuyerReturns(Long buyerId, int current, int size) {
        Page<OrderReturn> page = lambdaQuery()
                .eq(OrderReturn::getBuyerId, buyerId)
                .orderByDesc(OrderReturn::getApplyTime)
                .page(new Page<>(current, size));
        return toVOPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReturn(Long buyerId, Long returnId) {
        OrderReturn orderReturn = getOwnedReturn(buyerId, null, returnId, true);
        if (orderReturn.getStatus() != ReturnStatusEnum.APPLIED.getCode()) {
            throw new BusinessException("only applied returns can be canceled");
        }
        orderReturn.setStatus(ReturnStatusEnum.CANCELED.getCode());
        orderReturn.setHandleTime(LocalDateTime.now());
        updateById(orderReturn);
    }

    @Override
    public Page<ReturnVO> getSellerReturns(Long sellerId, int current, int size) {
        Page<OrderReturn> page = lambdaQuery()
                .eq(OrderReturn::getSellerId, sellerId)
                .orderByDesc(OrderReturn::getApplyTime)
                .page(new Page<>(current, size));
        return toVOPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturn(Long sellerId, Long returnId, String note) {
        OrderReturn orderReturn = getOwnedReturn(null, sellerId, returnId, false);
        if (orderReturn.getStatus() != ReturnStatusEnum.APPLIED.getCode()) {
            throw new BusinessException("only applied returns can be approved");
        }
        orderReturn.setStatus(ReturnStatusEnum.APPROVED.getCode());
        orderReturn.setHandleNote(note);
        orderReturn.setHandleTime(LocalDateTime.now());
        updateById(orderReturn);
        // Mock refund: restore stock immediately.
        productService.increaseStock(orderReturn.getProductId(), orderReturn.getQuantity());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReturn(Long sellerId, Long returnId, String note) {
        OrderReturn orderReturn = getOwnedReturn(null, sellerId, returnId, false);
        if (orderReturn.getStatus() != ReturnStatusEnum.APPLIED.getCode()) {
            throw new BusinessException("only applied returns can be rejected");
        }
        orderReturn.setStatus(ReturnStatusEnum.REJECTED.getCode());
        orderReturn.setHandleNote(note);
        orderReturn.setHandleTime(LocalDateTime.now());
        updateById(orderReturn);
    }

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

    private Page<ReturnVO> toVOPage(Page<OrderReturn> page) {
        Page<ReturnVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

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
