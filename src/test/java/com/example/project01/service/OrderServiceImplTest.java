package com.example.project01.service;

import com.example.project01.dto.OrderCreateRequest;
import com.example.project01.entity.Cart;
import com.example.project01.entity.Order;
import com.example.project01.entity.OrderItem;
import com.example.project01.entity.Product;
import com.example.project01.mapper.OrderMapper;
import com.example.project01.service.Impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private OrderEventService orderEventService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
    }

    @Test
    void createOrderFromCart() {
        Cart cart = new Cart();
        cart.setId(5L);
        cart.setBuyerId(1L);
        cart.setProductId(10L);
        cart.setNum(2);
        cart.setChecked(true);
        when(cartService.getCartList(1L)).thenReturn(List.of(cart));

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 15");
        product.setPrice(new BigDecimal("5999.00"));
        product.setStatus(1);
        when(productService.getProductById(10L)).thenReturn(product);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setIdempotencyKey("order-key-001");
        request.setReceiverName("Tom");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("Beijing");

        orderService.createOrder(1L, request);

        verify(productService).decreaseStock(10L, 2);
        verify(cartService).removeSelected(1L, List.of(5L));
        verify(orderEventService).publishOrderCreated(any(Order.class));
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemService).save(itemCaptor.capture());
        assertEquals("iPhone 15", itemCaptor.getValue().getProductName());
        assertEquals(new BigDecimal("5999.00"), itemCaptor.getValue().getPrice());
        assertEquals(new BigDecimal("11998.00"), itemCaptor.getValue().getSubtotal());
    }

    @Test
    void createOrderWithEmptyCartFails() {
        when(cartService.getCartList(1L)).thenReturn(List.of());
        OrderCreateRequest request = new OrderCreateRequest();
        request.setIdempotencyKey("order-key-002");
        request.setReceiverName("Tom");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("Beijing");

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> orderService.createOrder(1L, request));

        verify(orderMapper, never()).insert(any(Order.class));
    }
}
