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
import com.example.project01.entity.ProductReview;
import com.example.project01.entity.UserBuyer;
import com.example.project01.entity.UserSeller;
import com.example.project01.mapper.OrderMapper;
import com.example.project01.mapper.ProductReviewMapper;
import com.example.project01.service.CartService;
import com.example.project01.service.OrderItemService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.service.UserBuyerService;
import com.example.project01.service.UserSellerService;
import com.example.project01.service.OrderEventService;
import com.example.project01.vo.OrderVO;
import com.example.project01.vo.OrderReviewVO;
import com.example.project01.vo.SellerStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    private final ProductReviewMapper productReviewMapper;
    private final UserBuyerService userBuyerService;
    private final UserSellerService userSellerService;

    @Override
    public Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status,
                                    Boolean reviewed) {
        // Buyers can only see their own orders.
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, buyerId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (Boolean.TRUE.equals(reviewed)) {
            wrapper.apply("id IN (SELECT DISTINCT order_id FROM product_review WHERE buyer_id = {0})",
                    buyerId);
        } else if (Boolean.FALSE.equals(reviewed)) {
            wrapper.apply("id NOT IN (SELECT order_id FROM product_review WHERE buyer_id = {0})",
                    buyerId);
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
        vo.setSellerId(order.getSellerId());
        vo.setCheckoutGroupId(order.getCheckoutGroupId());
        UserBuyer buyer = userBuyerService.getById(order.getBuyerId());
        if (buyer != null) {
            vo.setBuyerUsername(buyer.getUsername());
            vo.setBuyerNickname(buyer.getNickname());
            vo.setBuyerPhone(buyer.getPhone());
            vo.setBuyerEmail(buyer.getEmail());
            vo.setBuyerAddress(buyer.getAddress());
        }
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatusEnum.fromCode(order.getStatus()).getDesc());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        List<ProductReview> reviews = productReviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getOrderId, order.getId())
                        .orderByDesc(ProductReview::getCreateTime));
        vo.setReviewedItemIds(reviews.stream()
                .map(ProductReview::getOrderItemId)
                .toList());
        vo.setReviews(reviews.stream().map(this::toOrderReviewVO).toList());
        if (order.getSellerId() != null) {
            UserSeller seller = userSellerService.getById(order.getSellerId());
            if (seller != null) {
                String sellerName = org.springframework.util.StringUtils.hasText(seller.getShopName())
                        ? seller.getShopName() : seller.getUsername();
                vo.setSellerName(sellerName);
                vo.setShopNames(List.of(sellerName));
            }
        } else if (!items.isEmpty()) {
            List<Long> productIds = items.stream().map(OrderItem::getProductId).toList();
            List<Long> sellerIds = productService.listByIds(productIds).stream()
                    .map(Product::getSellerId)
                    .distinct()
                    .toList();
            if (!sellerIds.isEmpty()) {
                vo.setSellerId(sellerIds.size() == 1 ? sellerIds.get(0) : null);
                List<String> shopNames = userSellerService.listByIds(sellerIds).stream()
                        .map(seller -> org.springframework.util.StringUtils.hasText(seller.getShopName())
                                ? seller.getShopName() : seller.getUsername())
                        .toList();
                vo.setShopNames(shopNames);
                if (shopNames.size() == 1) {
                    vo.setSellerName(shopNames.get(0));
                }
            }
        }
        if (vo.getShopNames() == null) {
            vo.setShopNames(List.of());
        }
        vo.setItems(items);
        return vo;
    }

    private OrderReviewVO toOrderReviewVO(ProductReview review) {
        OrderReviewVO vo = new OrderReviewVO();
        vo.setId(review.getId());
        vo.setOrderItemId(review.getOrderItemId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setReplyContent(review.getReplyContent());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Order> createOrder(Long buyerId, OrderCreateRequest request) {
        // One checkout submission can create one independent order per seller.
        List<Order> existingGroup = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getCheckoutGroupId, request.getIdempotencyKey())
                .orderByAsc(Order::getId));
        if (!existingGroup.isEmpty()) {
            return existingGroup;
        }
        // Compatibility for single legacy orders created before split orders.
        Order legacy = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getIdempotencyKey, request.getIdempotencyKey())
                .last("LIMIT 1"));
        if (legacy != null) {
            return List.of(legacy);
        }
        UserBuyer buyer = userBuyerService.getById(buyerId);
        if (buyer == null
                || !org.springframework.util.StringUtils.hasText(buyer.getNickname())
                || !org.springframework.util.StringUtils.hasText(buyer.getPhone())
                || !org.springframework.util.StringUtils.hasText(buyer.getEmail())
                || !org.springframework.util.StringUtils.hasText(buyer.getAddress())) {
            throw new BusinessException(ResultCode.BUYER_PROFILE_INCOMPLETE);
        }

        List<Cart> cartList = cartService.getCartList(buyerId).stream()
                .filter(c -> c.getChecked() == null || c.getChecked())
                .toList();
        if (cartList.isEmpty()) {
            throw new BusinessException("购物车为空，无法创建订单");
        }

        Map<Long, List<CheckoutLine>> linesBySeller = new TreeMap<>();
        // Validate every checked cart line before creating the order.
        for (Cart cart : cartList) {
            Product product = productService.getProductById(cart.getProductId());
            if (product == null || product.getStatus() != ProductStatusEnum.ON_SALE.getCode()) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            if (product.getSellerId() == null) {
                throw new BusinessException("商品所属卖家不存在");
            }
            linesBySeller.computeIfAbsent(product.getSellerId(), ignored -> new ArrayList<>())
                    .add(new CheckoutLine(cart, product));
        }

        List<Order> createdOrders = new ArrayList<>();
        for (Map.Entry<Long, List<CheckoutLine>> entry : linesBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<CheckoutLine> checkoutLines = entry.getValue();
            BigDecimal totalAmount = checkoutLines.stream()
                    .map(line -> line.product().getPrice()
                            .multiply(BigDecimal.valueOf(line.cart().getNum())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Order order = new Order();
            order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
            order.setCheckoutGroupId(request.getIdempotencyKey());
            order.setIdempotencyKey(subOrderIdempotencyKey(
                    request.getIdempotencyKey(), sellerId));
            order.setBuyerId(buyerId);
            order.setSellerId(sellerId);
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
            createdOrders.add(order);
        }

        cartService.removeSelected(buyerId, cartList.stream().map(Cart::getId).toList());
        return createdOrders;
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
            throw new BusinessException("只有待付款订单可以取消");
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
            throw new BusinessException("当前订单状态不允许执行该操作");
        }
        orderEventService.publishOrderPaid(order);
    }

    @Override
    public Page<Order> getSellerOrderPage(Long sellerId, int current, int size, Integer status) {
        // New split orders have seller_id directly. Legacy orders fall back to
        // product ownership so historical data remains visible.
        Page<Order> page = new Page<>(current, size);
        List<Long> productIds = sellerProductIds(sellerId);
        List<Long> orderIds = productIds.isEmpty()
                ? List.of()
                : orderItemService.lambdaQuery()
                        .in(OrderItem::getProductId, productIds)
                        .list()
                        .stream()
                        .map(OrderItem::getOrderId)
                        .distinct()
                        .toList();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            w.eq(Order::getSellerId, sellerId);
            if (!orderIds.isEmpty()) {
                w.or().in(Order::getId, orderIds);
            }
        });
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
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
        if (!sellerOwnsOrder(order, sellerId, items)) {
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
                .set(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                .set(Order::getShipTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException("只有已付款订单可以发货");
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
            throw new BusinessException("只有已发货订单可以确认收货");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCompleteShippedOrders(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        List<Order> shippedOrders = lambdaQuery()
                .eq(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                .isNotNull(Order::getShipTime)
                .le(Order::getShipTime, cutoff)
                .last("LIMIT 100")
                .list();

        int completed = 0;
        for (Order order : shippedOrders) {
            boolean updated = update(new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, order.getId())
                    .eq(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                    .isNotNull(Order::getShipTime)
                    .le(Order::getShipTime, cutoff)
                    .set(Order::getStatus, OrderStatusEnum.COMPLETED.getCode())
                    .set(Order::getCompleteTime, LocalDateTime.now()));
            if (updated) {
                completed++;
            }
        }
        return completed;
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
            throw new BusinessException("当前订单状态不能强制关单");
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
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
        if (!sellerOwnsOrder(order, sellerId, items)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    private boolean sellerOwnsOrder(Order order, Long sellerId, List<OrderItem> items) {
        if (order.getSellerId() != null) {
            return sellerId.equals(order.getSellerId());
        }
        List<Long> productIds = sellerProductIds(sellerId);
        if (productIds.isEmpty() || items.isEmpty()) {
            return false;
        }
        return items.stream()
                .map(OrderItem::getProductId)
                .anyMatch(productIds::contains);
    }

    private String subOrderIdempotencyKey(String checkoutGroupId, Long sellerId) {
        String suffix = "-seller-" + sellerId;
        int maxPrefixLength = 64 - suffix.length();
        String prefix = checkoutGroupId.length() > maxPrefixLength
                ? checkoutGroupId.substring(0, maxPrefixLength)
                : checkoutGroupId;
        return prefix + suffix;
    }
}
