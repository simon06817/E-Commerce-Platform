package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewIntegrationTest extends IntegrationTestSupport {

    @Test
    void buyerCanReviewAndSellerCanReply() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        String sellerToken = login("SELLER", "seller01", "123456");

        addToCart(buyerToken, 1, 1);
        long orderId = createOrder(buyerToken);
        mockMvc.perform(put("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/seller/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        JsonNode items = orderDetail(buyerToken, orderId).path("items");
        long orderItemId = itemIdForProduct(items, 1);

        MvcResult review = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":" + orderItemId
                                + ",\"rating\":5,\"content\":\"very good\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long reviewId = data(review).path("id").asLong();

        mockMvc.perform(put("/api/seller/reviews/" + reviewId + "/reply")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"thanks for your feedback\"}"))
                .andExpect(status().isOk());

        MvcResult reviews = mockMvc.perform(get("/api/products/1/reviews"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode reviewPage = data(reviews);
        assertEquals(1, reviewPage.path("total").asInt());
        assertEquals("thanks for your feedback",
                reviewPage.path("records").get(0).path("replyContent").asText());

        MvcResult summary = mockMvc.perform(get("/api/products/1/reviews/summary"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(5.0, data(summary).path("averageRating").asDouble(), 0.01);
        assertEquals(1, data(summary).path("reviewCount").asInt());
    }
}
