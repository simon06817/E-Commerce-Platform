package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderLifecycleIntegrationTest extends IntegrationTestSupport {

    @Test
    void buyerCanAddToCartAndCreateOrder() throws Exception {
        String token = login("BUYER", "buyer01", "123456");
        addToCart(token, 1, 2);

        MvcResult orderResult = createOrderResult(token, "create-flow-" + UUID.randomUUID());
        JsonNode order = data(orderResult);
        assertEquals(1, order.path("items").size());
        assertEquals("iPhone 15", order.path("items").get(0).path("productName").asText());
        assertEquals(11998.00, order.path("totalAmount").asDouble(), 0.01);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cancelOrderRestoresStock() throws Exception {
        String token = login("BUYER", "buyer01", "123456");
        int beforeStock = productStock(1);

        addToCart(token, 1, 1);
        long orderId = createOrder(token);

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertEquals(beforeStock, productStock(1));
    }

    @Test
    void duplicateCreateOrderWithSameIdempotencyKeyReturnsSameOrder() throws Exception {
        String token = login("BUYER", "buyer01", "123456");
        addToCart(token, 1, 1);

        String idempotencyKey = "duplicate-order-" + UUID.randomUUID();
        long firstId = data(createOrderResult(token, idempotencyKey)).path("id").asLong();
        long secondId = data(createOrderResult(token, idempotencyKey)).path("id").asLong();

        assertEquals(firstId, secondId);
    }

    @Test
    void sellerShipsAndBuyerConfirmsOrder() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        addToCart(buyerToken, 1, 1);
        long orderId = createOrder(buyerToken);

        mockMvc.perform(put("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        String sellerToken = login("SELLER", "seller01", "123456");
        mockMvc.perform(put("/api/seller/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        assertEquals(3, orderDetail(buyerToken, orderId).path("status").asInt());

        String otherSellerToken = login("SELLER", "seller02", "123456");
        mockMvc.perform(get("/api/seller/orders")
                        .header("Authorization", "Bearer " + otherSellerToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode page = objectMapper.readTree(
                            result.getResponse().getContentAsString()).path("data");
                    assertEquals(0, page.path("total").asInt());
                });
    }
}
