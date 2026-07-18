package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.Cart;
import com.example.project01.entity.Order;
import com.example.project01.entity.Product;
import com.example.project01.mapper.OrderMapper;
import com.example.project01.service.CartService;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;

    @Override
    public Page<Order> getOrderPage(int current, int size, Long userId, String status) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return this.page(page, wrapper);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, String address) {
        // 1. 获取用户购物车中选中的商品
        List<Cart> cartList = cartService.getCartList(userId);
        if (cartList.isEmpty()) {
            throw new RuntimeException("购物车为空");
        }
        // 2. 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart cart : cartList) {
            Product product = productService.getById(cart.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new RuntimeException("商品【" + cart.getProductId() + "】已下架或不存在");
            }
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getNum()));
            totalAmount = totalAmount.add(itemTotal);
        }
        // 3. 生成订单
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAddress(address);
        order.setTotalPrice(totalAmount);
        order.setStatus(0); // 0-待支付
        order.setCreateTime(LocalDateTime.now());
        this.save(order);
        // 4. 扣减库存（简化处理，实际应生成订单明细表）
        for (Cart cart : cartList) {
            productService.decreaseStock(cart.getProductId(), cart.getNum());
        }
        // 5. 清空购物车
        cartService.clearCart(userId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("只有待支付订单可以取消");
        }
        order.setStatus(2); // 2-已取消
        this.updateById(order);
        // 实际应恢复库存（此处简化）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确");
        }
        order.setStatus(1); // 1-已支付
        order.setPayTime(LocalDateTime.now());
        this.updateById(order);
    }
}
