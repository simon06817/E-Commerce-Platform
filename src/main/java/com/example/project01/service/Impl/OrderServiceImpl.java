package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.OrderCreateRequest;
import com.example.project01.entity.Cart;
import com.example.project01.entity.Order;
import com.example.project01.entity.OrderItem;
import com.example.project01.entity.Product;
import com.example.project01.mapper.OrderMapper;
import com.example.project01.service.CartService;
import com.example.project01.service.OrderItemService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CartService cartService;
    private final ProductService productService;
    private final OrderItemService orderItemService;

    @Override
    public Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, buyerId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public OrderVO getOrderDetail(Long buyerId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerId(order.getBuyerId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setItems(items);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long buyerId, OrderCreateRequest request) {
        List<Cart> cartList = cartService.getCartList(buyerId).stream()
                .filter(c -> c.getChecked() == null || c.getChecked())
                .toList();
        if (cartList.isEmpty()) {
            throw new BusinessException("cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart cart : cartList) {
            Product product = productService.getProductById(cart.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cart.getNum())));
        }

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setBuyerId(buyerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        save(order);

        for (Cart cart : cartList) {
            Product product = productService.getProductById(cart.getProductId());
            productService.decreaseStock(product.getId(), cart.getNum());

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getNum());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getNum())));
            orderItemService.save(item);
        }

        cartService.clearCart(buyerId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long buyerId, Long orderId) {
        Order order = getOwnedOrder(buyerId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("only unpaid order can be canceled");
        }
        order.setStatus(4);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long buyerId, Long orderId) {
        Order order = getOwnedOrder(buyerId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("order status is invalid");
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    private Order getOwnedOrder(Long buyerId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }
}
