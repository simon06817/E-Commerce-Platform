package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewIntegrationTest extends IntegrationTestSupport {

    @Test
    void buyerCannotReviewBeforeConfirmingReceipt() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        String sellerToken = login("SELLER", "seller01", "123456");

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

        MvcResult result = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":" + orderItemId
                                + ",\"rating\":5,\"content\":\"too early\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(1010, body(result).path("code").asInt());
    }

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

        MvcResult emptyReview = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":" + orderItemId
                                + ",\"rating\":5,\"content\":\"   \"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(400, body(emptyReview).path("code").asInt());

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

        JsonNode reviewedOrder = orderDetail(buyerToken, orderId);
        assertEquals(1, reviewedOrder.path("reviews").size());
        assertEquals("very good",
                reviewedOrder.path("reviews").get(0).path("content").asText());

        MvcResult reviewedOrders = mockMvc.perform(get("/api/orders")
                        .param("reviewed", "true")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode reviewedPage = data(reviewedOrders);
        assertTrue(reviewedPage.path("total").asInt() >= 1);
        boolean containsOrder = false;
        for (JsonNode order : reviewedPage.path("records")) {
            if (order.path("id").asLong() == orderId) {
                containsOrder = true;
                break;
            }
        }
        assertTrue(containsOrder);
    }

    @Test
    void refundedOrderItemCannotBeReviewed() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        String sellerToken = login("SELLER", "seller01", "123456");

        addToCart(buyerToken, 2, 1);
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
        long orderItemId = itemIdForProduct(items, 2);

        MvcResult createdReturn = mockMvc.perform(post("/api/returns")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":" + orderItemId
                                + ",\"reason\":\"refund before review\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long returnId = data(createdReturn).path("id").asLong();

        mockMvc.perform(put("/api/seller/returns/" + returnId + "/approve")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"approved\"}"))
                .andExpect(status().isOk());

        MvcResult review = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":" + orderItemId
                                + ",\"rating\":5,\"content\":\"refunded item\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(1012, body(review).path("code").asInt());
    }
}
