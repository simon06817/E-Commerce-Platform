package com.example.project01.integration;

import com.example.project01.common.BusinessException;
import com.example.project01.dto.ProductRequest;
import com.example.project01.entity.OutboxMessage;
import com.example.project01.entity.Product;
import com.example.project01.service.CartService;
import com.example.project01.service.OrderService;
import com.example.project01.service.OutboxMessageService;
import com.example.project01.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReliabilityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OutboxMessageService outboxMessageService;

    @Test
    void duplicatePaymentCreatesOnlyOnePaidEvent() throws Exception {
        cartService.clearCart(1L);
        String token = login("BUYER", "buyer01", "123456");
        addToCart(token, 1, 1);
        long orderId = createOrder(token);

        orderService.payOrder(1L, orderId);
        assertThrows(BusinessException.class, () -> orderService.payOrder(1L, orderId));

        long paidEvents = outboxMessageService.lambdaQuery()
                .eq(OutboxMessage::getOrderId, orderId)
                .eq(OutboxMessage::getEventType, "ORDER_PAID")
                .count();
        assertEquals(1, paidEvents);
        assertEquals(1, orderService.getById(orderId).getStatus());
    }

    @Test
    void checkoutRollsBackOrdersStockAndOutboxWhenLaterSellerStockIsInsufficient()
            throws Exception {
        cartService.clearCart(1L);
        String token = login("BUYER", "buyer01", "123456");
        int firstSellerStock = productService.getById(1L).getStock();
        long outboxBefore = outboxMessageService.count();

        ProductRequest request = new ProductRequest();
        request.setName("rollback-product-" + UUID.randomUUID());
        request.setDescription("reliability rollback test");
        request.setPrice(new BigDecimal("19.90"));
        request.setStock(1);
        request.setCategoryId(1L);
        request.setStatus(1);
        productService.createProduct(2L, request);
        Product secondSellerProduct = productService.lambdaQuery()
                .eq(Product::getName, request.getName())
                .one();
        assertTrue(secondSellerProduct != null);

        String checkoutGroupId = "rollback-" + UUID.randomUUID();
        try {
            addToCart(token, 1, 1);
            addToCart(token, secondSellerProduct.getId().intValue(), 2);

            MvcResult result = mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content("{\"idempotencyKey\":\"" + checkoutGroupId
                                    + "\",\"receiverName\":\"Tom\","
                                    + "\"receiverPhone\":\"13800000000\","
                                    + "\"receiverAddress\":\"Beijing\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals(1003, body(result).path("code").asInt());
            assertEquals(0, orderService.lambdaQuery()
                    .eq(com.example.project01.entity.Order::getCheckoutGroupId, checkoutGroupId)
                    .count());
            assertEquals(firstSellerStock, productService.getById(1L).getStock());
            assertEquals(1, productService.getById(secondSellerProduct.getId()).getStock());
            assertEquals(outboxBefore, outboxMessageService.count());
            assertEquals(2, cartService.getCartList(1L).size());
        } finally {
            cartService.clearCart(1L);
            productService.deleteProduct(2L, secondSellerProduct.getId());
        }
    }
}
