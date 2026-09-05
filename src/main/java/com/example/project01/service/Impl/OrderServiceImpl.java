package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.OrderStatusEnum;
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
import com.example.project01.service.OrderEventService;
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
    private final OrderEventService orderEventService;

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
        return toVO(order, items);
    }

    private OrderVO toVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerId(order.getBuyerId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatusEnum.fromCode(order.getStatus()).getDesc());
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
        Order existing = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getIdempotencyKey, request.getIdempotencyKey()));
        if (existing != null) {
            return existing;
        }

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
        order.setIdempotencyKey(request.getIdempotencyKey());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatusEnum.UNPAID.getCode());
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

        orderEventService.publishOrderCreated(order);
        cartService.removeSelected(buyerId, cartList.stream().map(Cart::getId).toList());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders(int expireMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        List<Order> expired = lambdaQuery()
                .eq(Order::getStatus, OrderStatusEnum.UNPAID.getCode())
                .lt(Order::getCreateTime, cutoff)
                .last("LIMIT 100")
                .list();

        int closed = 0;
        for (Order order : expired) {
            try {
                cancelOrder(order.getBuyerId(), order.getId());
                closed++;
            } catch (BusinessException ignored) {
                // already paid or canceled by another request
            }
        }
        return closed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long buyerId, Long orderId) {
        getOwnedOrder(buyerId, orderId);
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.UNPAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.CANCELED.getCode()));
        if (!updated) {
            throw new BusinessException("only unpaid order can be canceled");
        }
        restoreStock(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long buyerId, Long orderId) {
        Order order = getOwnedOrder(buyerId, orderId);
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.UNPAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getPayTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException("order status is invalid");
        }
        orderEventService.publishOrderPaid(order);
    }

    @Override
    public Page<Order> getSellerOrderPage(Long sellerId, int current, int size, Integer status) {
        Page<Order> page = new Page<>(current, size);
        List<Long> productIds = sellerProductIds(sellerId);
        if (productIds.isEmpty()) {
            return page;
        }
        List<Long> orderIds = orderItemService.lambdaQuery()
                .in(OrderItem::getProductId, productIds)
                .list()
                .stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return page;
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getId, orderIds);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public OrderVO getSellerOrderDetail(Long sellerId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<Long> productIds = sellerProductIds(sellerId);
        if (productIds.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .in(OrderItem::getProductId, productIds)
                .list();
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long sellerId, Long orderId) {
        ensureSellerOrder(orderId, sellerId);
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.SHIPPED.getCode()));
        if (!updated) {
            throw new BusinessException("only paid order can be shipped");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long buyerId, Long orderId) {
        getOwnedOrder(buyerId, orderId);
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                .set(Order::getStatus, OrderStatusEnum.COMPLETED.getCode()));
        if (!updated) {
            throw new BusinessException("only shipped order can be confirmed");
        }
    }

    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
        for (OrderItem item : items) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }
    }

    private Order getOwnedOrder(Long buyerId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    private List<Long> sellerProductIds(Long sellerId) {
        return productService.lambdaQuery()
                .eq(Product::getSellerId, sellerId)
                .list()
                .stream()
                .map(Product::getId)
                .toList();
    }

    private void ensureSellerOrder(Long orderId, Long sellerId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<Long> productIds = sellerProductIds(sellerId);
        if (productIds.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        long count = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .in(OrderItem::getProductId, productIds)
                .count();
        if (count == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }
}
