package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.OrderStatusEnum;
import com.example.project01.common.ProductStatusEnum;
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
import com.example.project01.vo.SellerStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core order service. Implements idempotent checkout, stock-sensitive
 * cancellation, buyer payment/receipt and seller shipping flows.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CartService cartService;
    private final ProductService productService;
    private final OrderItemService orderItemService;
    private final OrderEventService orderEventService;

    @Override
    public Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status) {
        // Buyers can only see their own orders.
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
        vo.setCompleteTime(order.getCompleteTime());
        vo.setItems(items);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long buyerId, OrderCreateRequest request) {
        // Idempotency guard: the same key always returns the original order.
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
        List<CheckoutLine> checkoutLines = new ArrayList<>();
        // Validate every checked cart line before creating the order.
        for (Cart cart : cartList) {
            Product product = productService.getProductById(cart.getProductId());
            if (product == null || product.getStatus() != ProductStatusEnum.ON_SALE.getCode()) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            checkoutLines.add(new CheckoutLine(cart, product));
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

        for (CheckoutLine line : checkoutLines) {
            Cart cart = line.cart();
            Product product = line.product();
            // Reuse the already loaded product; the update itself is still atomic.
            productService.decreaseStock(product, cart.getNum());

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
        // Batch scan keeps each scheduler run bounded.
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
        // Conditional update makes cancellation safe against concurrent pay/cancel.
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
        // Only unpaid orders can transition to paid.
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
        // Resolve the seller's products first, then find orders containing them.
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
        // A seller may only inspect orders containing own products.
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
        // Shipping is allowed only after payment.
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
        // Receipt confirmation is allowed only after shipping.
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                .set(Order::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .set(Order::getCompleteTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException("only shipped order can be confirmed");
        }
    }

    @Override
    public Page<Order> getAdminOrderPage(int current, int size, String orderNo,
                                         Long buyerId, Integer status) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (org.springframework.util.StringUtils.hasText(orderNo)) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        if (buyerId != null) {
            wrapper.eq(Order::getBuyerId, buyerId);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public OrderVO getAdminOrderDetail(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
        return toVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceCancelOrder(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // Completed and already-canceled orders cannot be force-canceled.
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .in(Order::getStatus,
                        OrderStatusEnum.UNPAID.getCode(),
                        OrderStatusEnum.PAID.getCode(),
                        OrderStatusEnum.SHIPPED.getCode())
                .set(Order::getStatus, OrderStatusEnum.CANCELED.getCode()));
        if (!updated) {
            throw new BusinessException("order status cannot be force canceled");
        }
        restoreStock(orderId);
    }

    @Override
    public SellerStatsVO getSellerStats(Long sellerId, String range) {
        List<Long> productIds = sellerProductIds(sellerId);
        if (productIds.isEmpty()) {
            return new SellerStatsVO(BigDecimal.ZERO, 0L, 0L, BigDecimal.ZERO, range);
        }
        List<OrderItem> items = orderItemService.lambdaQuery()
                .in(OrderItem::getProductId, productIds)
                .list();
        Map<Long, List<OrderItem>> itemsByOrder = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        if (itemsByOrder.isEmpty()) {
            return new SellerStatsVO(BigDecimal.ZERO, 0L, 0L, BigDecimal.ZERO, range);
        }
        LocalDateTime since = statsSince(range);
        List<Order> orders = listByIds(itemsByOrder.keySet());
        BigDecimal revenue = BigDecimal.ZERO;
        long itemCount = 0;
        long orderCount = 0;
        for (Order order : orders) {
            boolean validStatus = order.getStatus() == OrderStatusEnum.PAID.getCode()
                    || order.getStatus() == OrderStatusEnum.SHIPPED.getCode()
                    || order.getStatus() == OrderStatusEnum.COMPLETED.getCode();
            boolean inRange = since == null || (order.getCreateTime() != null
                    && !order.getCreateTime().isBefore(since));
            if (!validStatus || !inRange) {
                continue;
            }
            orderCount++;
            for (OrderItem item : itemsByOrder.get(order.getId())) {
                revenue = revenue.add(item.getSubtotal());
                itemCount += item.getQuantity();
            }
        }
        BigDecimal average = orderCount == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);
        return new SellerStatsVO(revenue, orderCount, itemCount, average, range);
    }

    private LocalDateTime statsSince(String range) {
        if (range == null) {
            return null;
        }
        return switch (range) {
            case "today" -> LocalDateTime.now().toLocalDate().atStartOfDay();
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> null;
        };
    }

    private record CheckoutLine(Cart cart, Product product) {
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
