package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.common.OrderStatusEnum;
import com.ecommerce.platform.common.ProductStatusEnum;
import com.ecommerce.platform.common.ResultCode;
import com.ecommerce.platform.dto.OrderCreateRequest;
import com.ecommerce.platform.entity.Cart;
import com.ecommerce.platform.entity.Order;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.entity.ProductReview;
import com.ecommerce.platform.entity.UserBuyer;
import com.ecommerce.platform.entity.UserSeller;
import com.ecommerce.platform.mapper.OrderMapper;
import com.ecommerce.platform.mapper.ProductReviewMapper;
import com.ecommerce.platform.service.CartService;
import com.ecommerce.platform.service.OrderItemService;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.service.ProductService;
import com.ecommerce.platform.service.UserBuyerService;
import com.ecommerce.platform.service.UserSellerService;
import com.ecommerce.platform.service.OrderEventService;
import com.ecommerce.platform.vo.OrderVO;
import com.ecommerce.platform.vo.OrderReviewVO;
import com.ecommerce.platform.vo.SellerStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/** 订单服务实现。 */
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

    /** 分页查询买家订单。 */
    @Override
    public Page<Order> getOrderPage(Long buyerId, int current, int size, Integer status,
                                    Boolean reviewed) {
        // 买家只能查看自己的订单。
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

    /** 分页查询买家订单详情。 */
    @Override
    public Page<OrderVO> getOrderPageDetails(Long buyerId, int current, int size,
                                             Integer status, Boolean reviewed) {
        return toVOPage(getOrderPage(buyerId, current, size, status, reviewed));
    }

    /** 查询订单详情。 */
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

    /** 转换业务返回对象。 */
    private OrderVO toVO(Order order, List<OrderItem> items) {
        UserBuyer buyer = userBuyerService.getById(order.getBuyerId());
        UserSeller seller = order.getSellerId() == null
                ? null : userSellerService.getById(order.getSellerId());
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .toList();
        Map<Long, Product> products = productIds.isEmpty()
                ? Map.of()
                : productService.listByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, product -> product));
        List<ProductReview> reviews = productReviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getOrderId, order.getId())
                        .orderByDesc(ProductReview::getCreateTime));
        Map<Long, UserBuyer> buyers = buyer == null
                ? Map.of() : Map.of(buyer.getId(), buyer);
        Map<Long, UserSeller> sellers = seller == null
                ? Map.of() : Map.of(seller.getId(), seller);
        Map<Long, List<ProductReview>> reviewsByOrder = Map.of(order.getId(), reviews);
        return toVO(order, items, buyers, sellers, products, reviewsByOrder);
    }

    /** 转换业务返回对象。 */
    private OrderVO toVO(Order order, List<OrderItem> items,
                         Map<Long, UserBuyer> buyers,
                         Map<Long, UserSeller> sellers,
                         Map<Long, Product> products,
                         Map<Long, List<ProductReview>> reviewsByOrder) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setCheckoutGroupId(order.getCheckoutGroupId());
        UserBuyer buyer = buyers.get(order.getBuyerId());
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
        List<ProductReview> reviews = reviewsByOrder.getOrDefault(order.getId(), List.of());
        vo.setReviewedItemIds(reviews.stream()
                .map(ProductReview::getOrderItemId)
                .toList());
        vo.setReviews(reviews.stream().map(this::toOrderReviewVO).toList());
        if (order.getSellerId() != null) {
            UserSeller seller = sellers.get(order.getSellerId());
            if (seller != null) {
                String sellerName = org.springframework.util.StringUtils.hasText(seller.getShopName())
                        ? seller.getShopName() : seller.getUsername();
                vo.setSellerName(sellerName);
                vo.setShopNames(List.of(sellerName));
            }
        } else if (!items.isEmpty()) {
            List<Long> sellerIds = items.stream()
                    .map(item -> products.get(item.getProductId()))
                    .filter(java.util.Objects::nonNull)
                    .map(Product::getSellerId)
                    .distinct()
                    .toList();
            if (!sellerIds.isEmpty()) {
                vo.setSellerId(sellerIds.size() == 1 ? sellerIds.get(0) : null);
                List<String> shopNames = sellerIds.stream()
                        .map(sellers::get)
                        .filter(java.util.Objects::nonNull)
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

    /** 转换分页返回对象。 */
    private Page<OrderVO> toVOPage(Page<Order> orderPage) {
        List<Order> orders = orderPage.getRecords();
        Page<OrderVO> result = new Page<>(
                orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        if (orders == null || orders.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderItemService.lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .list();
        Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<Long> buyerIds = orders.stream()
                .map(Order::getBuyerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserBuyer> buyers = buyerIds.isEmpty()
                ? Map.of()
                : userBuyerService.listByIds(buyerIds).stream()
                        .collect(Collectors.toMap(UserBuyer::getId, buyer -> buyer));

        Set<Long> productIds = allItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());
        Map<Long, Product> products = productIds.isEmpty()
                ? Map.of()
                : productService.listByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, product -> product));

        Set<Long> sellerIds = orders.stream()
                .map(Order::getSellerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        sellerIds.addAll(products.values().stream()
                .map(Product::getSellerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, UserSeller> sellers = sellerIds.isEmpty()
                ? Map.of()
                : userSellerService.listByIds(sellerIds).stream()
                        .collect(Collectors.toMap(UserSeller::getId, seller -> seller));

        List<ProductReview> reviews = productReviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>()
                        .in(ProductReview::getOrderId, orderIds)
                        .orderByDesc(ProductReview::getCreateTime));
        Map<Long, List<ProductReview>> reviewsByOrder = reviews.stream()
                .collect(Collectors.groupingBy(ProductReview::getOrderId));

        result.setRecords(orders.stream()
                .map(order -> toVO(
                        order,
                        itemsByOrder.getOrDefault(order.getId(), List.of()),
                        buyers,
                        sellers,
                        products,
                        reviewsByOrder))
                .toList());
        return result;
    }

    /** 转换订单评价返回对象。 */
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

    /** 创建订单。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Order> createOrder(Long buyerId, OrderCreateRequest request) {
        try {
            return createOrderInTransaction(buyerId, request);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /** 在事务内按卖家拆分并创建订单。 */
    private List<Order> createOrderInTransaction(Long buyerId, OrderCreateRequest request) {
        // 一次结算可按卖家生成多张订单。
        List<Order> existingGroup = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getCheckoutGroupId, request.getIdempotencyKey())
                .orderByAsc(Order::getId));
        if (!existingGroup.isEmpty()) {
            return existingGroup;
        }
        // 兼容拆单前的历史订单。
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
        // 创建订单前校验所有已选购物车项。
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
                // 复用已查询的商品，库存更新仍由条件更新保证原子性。
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

    /** 关闭超时未支付订单。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders(int expireMinutes) {
        // 每轮定时扫描限制批量大小。
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
                // 已被其他请求支付或取消
            }
        }
        return closed;
    }

    /** 取消待付款订单并恢复库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long buyerId, Long orderId) {
        getOwnedOrder(buyerId, orderId);
        // 条件更新避免支付和取消并发冲突。
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.UNPAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.CANCELED.getCode()));
        if (!updated) {
            throw new BusinessException("只有待付款订单可以取消");
        }
        restoreStock(orderId);
    }

    /** 支付待付款订单。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long buyerId, Long orderId) {
        Order order = getOwnedOrder(buyerId, orderId);
        // 只有待付款订单可以支付。
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

    /** 分页查询卖家订单。 */
    @Override
    public Page<Order> getSellerOrderPage(Long sellerId, int current, int size, Integer status) {
        // 新拆单订单直接使用 seller_id，历史订单回退到
        // 商品归属，保证历史订单仍可查询。
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

    /** 分页查询卖家订单详情。 */
    @Override
    public Page<OrderVO> getSellerOrderPageDetails(Long sellerId, int current, int size,
                                                   Integer status) {
        return toVOPage(getSellerOrderPage(sellerId, current, size, status));
    }

    /** 查询卖家订单详情。 */
    @Override
    public OrderVO getSellerOrderDetail(Long sellerId, Long orderId) {
        // 卖家只能查看包含自己商品的订单。
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

    /** 卖家订单发货。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long sellerId, Long orderId) {
        ensureSellerOrder(orderId, sellerId);
        // 只有已支付订单可以发货。
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                .set(Order::getShipTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException("只有已付款订单可以发货");
        }
    }

    /** 确认订单收货。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long buyerId, Long orderId) {
        getOwnedOrder(buyerId, orderId);
        // 只有已发货订单可以确认收货。
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusEnum.SHIPPED.getCode())
                .set(Order::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .set(Order::getCompleteTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException("只有已发货订单可以确认收货");
        }
    }

    /** 自动完成已发货订单。 */
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

    /** 分页查询管理员订单。 */
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

    /** 查询管理员订单详情。 */
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

    /** 强制关闭订单并恢复库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceCancelOrder(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 已完成或已取消订单不能强制关单。
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

    /** 查询卖家收入统计。 */
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

    /** 计算统计起始时间。 */
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

    /** 恢复订单商品库存。 */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .list();
        for (OrderItem item : items) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }
    }

    /** 查询并校验订单归属。 */
    private Order getOwnedOrder(Long buyerId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    /** 查询卖家全部商品 ID。 */
    private List<Long> sellerProductIds(Long sellerId) {
        return productService.lambdaQuery()
                .eq(Product::getSellerId, sellerId)
                .list()
                .stream()
                .map(Product::getId)
                .toList();
    }

    /** 校验卖家是否有权访问订单。 */
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

    /** 判断卖家是否拥有订单商品。 */
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

    /** 生成拆单子订单幂等键。 */
    private String subOrderIdempotencyKey(String checkoutGroupId, Long sellerId) {
        String suffix = "-seller-" + sellerId;
        int maxPrefixLength = 64 - suffix.length();
        String prefix = checkoutGroupId.length() > maxPrefixLength
                ? checkoutGroupId.substring(0, maxPrefixLength)
                : checkoutGroupId;
        return prefix + suffix;
    }
}
