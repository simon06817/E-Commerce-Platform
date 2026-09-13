package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String login(String role, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role + "\",\"username\":\"" + username
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(200, body(result).path("code").asInt());
        return data(result).path("token").asText();
    }

    protected void addToCart(String token, int productId, int num) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"num\":" + num + "}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(200, body(result).path("code").asInt());
    }

    protected MvcResult createOrderResult(String token, String idempotencyKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + idempotencyKey
                                + "\",\"receiverName\":\"Tom\",\"receiverPhone\":\"13800000000\","
                                + "\"receiverAddress\":\"Beijing\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(200, body(result).path("code").asInt());
        return result;
    }

    protected long createOrder(String token) throws Exception {
        return data(createOrderResult(token, UUID.randomUUID().toString())).path("id").asLong();
    }

    protected int productStock(int productId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andReturn();
        return data(result).path("stock").asInt();
    }

    protected JsonNode orderDetail(String token, long orderId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return data(result);
    }

    protected long itemIdForProduct(JsonNode items, long productId) {
        for (JsonNode item : items) {
            if (item.path("productId").asLong() == productId) {
                return item.path("id").asLong();
            }
        }
        throw new AssertionError("item not found for product " + productId);
    }

    protected JsonNode data(MvcResult result) throws Exception {
        return body(result).path("data");
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
