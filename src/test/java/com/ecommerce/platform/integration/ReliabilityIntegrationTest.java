package com.ecommerce.platform.integration;

import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.event.OrderEventMessage;
import com.ecommerce.platform.service.CartService;
import com.ecommerce.platform.service.OrderService;
import com.ecommerce.platform.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ReliabilityIntegrationTest.EventCaptureConfig.class)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:reliabilitydb;MODE=MySQL;"
                + "DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1")
class ReliabilityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private EventCapture eventCapture;

    @BeforeEach
    void clearEvents() {
        eventCapture.clear();
    }

    @Test
    void duplicatePaymentCreatesOnlyOnePaidEvent() throws Exception {
        cartService.clearCart(1L);
        String token = login("BUYER", "buyer01", "123456");
        addToCart(token, 1, 1);
        long orderId = createOrder(token);

        orderService.payOrder(1L, orderId);
        assertThrows(BusinessException.class, () -> orderService.payOrder(1L, orderId));

        long paidEvents = eventCapture.events().stream()
                .filter(event -> event.orderId() == orderId && "ORDER_PAID".equals(event.eventType()))
                .count();
        assertEquals(1, paidEvents);
        assertEquals(1, orderService.getById(orderId).getStatus());
    }

    @Test
    void checkoutRollsBackOrdersStockAndEventsWhenLaterSellerStockIsInsufficient()
            throws Exception {
        cartService.clearCart(1L);
        String token = login("BUYER", "buyer01", "123456");
        int firstSellerStock = productService.getById(1L).getStock();
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
                    .andExpect(status().isConflict())
                    .andReturn();

            assertEquals(1003, body(result).path("code").asInt());
            assertEquals(0, orderService.lambdaQuery()
                    .eq(com.ecommerce.platform.entity.Order::getCheckoutGroupId, checkoutGroupId)
                    .count());
            assertEquals(firstSellerStock, productService.getById(1L).getStock());
            assertEquals(1, productService.getById(secondSellerProduct.getId()).getStock());
            assertTrue(eventCapture.events().isEmpty());
            assertEquals(2, cartService.getCartList(1L).size());
        } finally {
            cartService.clearCart(1L);
            productService.deleteProduct(2L, secondSellerProduct.getId());
        }
    }

    @TestConfiguration
    static class EventCaptureConfig {

        @Bean
        EventCapture eventCapture() {
            return new EventCapture();
        }
    }

    static class EventCapture {

        private final List<OrderEventMessage> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void onOrderEvent(OrderEventMessage event) {
            events.add(event);
        }

        List<OrderEventMessage> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
