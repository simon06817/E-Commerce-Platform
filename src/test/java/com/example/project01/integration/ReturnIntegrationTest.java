package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReturnIntegrationTest extends IntegrationTestSupport {

    @Test
    void approvedReturnRestoresStock() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        String sellerToken = login("SELLER", "seller01", "123456");
        int stockBefore = productStock(2);

        addToCart(buyerToken, 2, 1);
        long orderId = createOrder(buyerToken);
        mockMvc.perform(put("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/seller/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        JsonNode items = orderDetail(buyerToken, orderId).path("items");
        long orderItemId = itemIdForProduct(items, 2);

        MvcResult createdReturn = mockMvc.perform(post("/api/returns")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":" + orderItemId
                                + ",\"reason\":\"not suitable\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long returnId = data(createdReturn).path("id").asLong();

        mockMvc.perform(put("/api/seller/returns/" + returnId + "/approve")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"approved\"}"))
                .andExpect(status().isOk());

        assertEquals(stockBefore, productStock(2));
    }
}
