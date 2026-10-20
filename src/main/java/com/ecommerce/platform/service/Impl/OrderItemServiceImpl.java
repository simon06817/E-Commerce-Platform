package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.entity.OrderItem;
import com.ecommerce.platform.mapper.OrderItemMapper;
import com.ecommerce.platform.service.OrderItemService;
import org.springframework.stereotype.Service;

/** 订单明细服务实现。 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
