package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void buyerCanAddToCartAndCreateOrder() throws Exception {
        String token = login("BUYER", "buyer01", "123456");

        mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"num\":2}"))
                .andExpect(status().isOk());

        var orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"create-flow-001\",\"receiverName\":\"Tom\",\"receiverPhone\":\"13800000000\",\"receiverAddress\":\"Beijing\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(orderResult.getResponse().getContentAsString()).path("data");
        assertEquals(1, data.path("items").size());
        assertEquals("iPhone 15", data.path("items").get(0).path("productName").asText());
        assertEquals(11998.00, data.path("totalAmount").asDouble(), 0.01);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"BUYER\",\"username\":\"buyer01\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertEquals(1006, body.path("code").asInt());
                });
    }

    @Test
    void sellerCannotAccessBuyerCart() throws Exception {
        String token = login("SELLER", "seller01", "123456");
        mockMvc.perform(get("/api/carts").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListBuyers() throws Exception {
        String token = login("ADMIN", "admin", "admin");
        mockMvc.perform(get("/api/admin/users/buyers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cancelOrderRestoresStock() throws Exception {
        String token = login("BUYER", "buyer01", "123456");

        JsonNode beforeNode = objectMapper.readTree(
                mockMvc.perform(get("/api/products/1"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()).path("data");
        int beforeStock = beforeNode.path("stock").asInt();

        mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"num\":1}"))
                .andExpect(status().isOk());

        var orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"cancel-flow-001\",\"receiverName\":\"Tom\",\"receiverPhone\":\"13800000000\",\"receiverAddress\":\"Beijing\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode afterNode = objectMapper.readTree(
                mockMvc.perform(get("/api/products/1"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()).path("data");
        assertEquals(beforeStock, afterNode.path("stock").asInt());
    }

    @Test
    void duplicateCreateOrderWithSameIdempotencyKeyReturnsSameOrder() throws Exception {
        String token = login("BUYER", "buyer01", "123456");

        mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"num\":1}"))
                .andExpect(status().isOk());

        String body = "{\"idempotencyKey\":\"dup-key-001\",\"receiverName\":\"Tom\","
                + "\"receiverPhone\":\"13800000000\",\"receiverAddress\":\"Beijing\"}";

        var first = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        var second = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        long firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long secondId = objectMapper.readTree(second.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertEquals(firstId, secondId);
    }

    @Test
    void sellerShipsAndBuyerConfirmsOrder() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");

        mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"num\":1}"))
                .andExpect(status().isOk());

        var orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"phase3-flow-001\",\"receiverName\":\"Tom\","
                                + "\"receiverPhone\":\"13800000000\",\"receiverAddress\":\"Beijing\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        String sellerToken = login("SELLER", "seller01", "123456");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/seller/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        var detail = mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(detail.getResponse().getContentAsString()).path("data");
        assertEquals(3, data.path("status").asInt());

        String otherSellerToken = login("SELLER", "seller02", "123456");
        mockMvc.perform(get("/api/seller/orders")
                        .header("Authorization", "Bearer " + otherSellerToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
                    assertEquals(0, page.path("total").asInt());
                });
    }

    private String login(String role, String username, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role + "\",\"username\":\"" + username
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }
}
