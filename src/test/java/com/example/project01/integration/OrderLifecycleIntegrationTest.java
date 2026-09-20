package com.example.project01.integration;

import com.example.project01.entity.Order;
import com.example.project01.entity.Product;
import com.example.project01.entity.UserBuyer;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.project01.service.OrderService;
import com.example.project01.service.ProductService;
import com.example.project01.dto.ProductRequest;
import com.example.project01.service.UserBuyerService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderLifecycleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserBuyerService userBuyerService;

    @Autowired
    private ProductService productService;

    @Test
    void buyerCanAddToCartAndCreateOrder() throws Exception {
        String token = login("BUYER", "buyer01", "123456");
        addToCart(token, 1, 2);

        MvcResult orderResult = createOrderResult(token, "create-flow-" + UUID.randomUUID());
        JsonNode order = data(orderResult).get(0);
        assertEquals(1, order.path("items").size());
        assertEquals("iPhone 15", order.path("items").get(0).path("productName").asText());
        assertEquals(11998.00, order.path("totalAmount").asDouble(), 0.01);
        assertEquals("buyer01", order.path("buyerUsername").asText());
        assertEquals("Tech Store", order.path("shopNames").get(0).asText());

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
        JsonNode firstPayload = data(createOrderResult(token, idempotencyKey));
        JsonNode secondPayload = data(createOrderResult(token, idempotencyKey));
        long firstId = firstPayload.get(0).path("id").asLong();
        long secondId = secondPayload.get(0).path("id").asLong();

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
                    for (JsonNode order : page.path("records")) {
                        assertTrue(order.path("id").asLong() != orderId);
                    }
                });
    }

    @Test
    void shippedOrderAutoCompletesAfterConfiguredHours() throws Exception {
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

        Order shipped = orderService.getById(orderId);
        shipped.setShipTime(LocalDateTime.now().minusHours(25));
        orderService.updateById(shipped);

        assertTrue(orderService.autoCompleteShippedOrders(24) >= 1);
        assertEquals(3, orderService.getById(orderId).getStatus());
    }

    @Test
    void incompleteBuyerProfileCannotCreateOrder() throws Exception {
        UserBuyer buyer = userBuyerService.getById(2L);
        String originalEmail = buyer.getEmail();
        userBuyerService.update(new LambdaUpdateWrapper<UserBuyer>()
                .eq(UserBuyer::getId, buyer.getId())
                .set(UserBuyer::getEmail, null));
        try {
            String buyerToken = login("BUYER", "buyer02", "123456");
            addToCart(buyerToken, 1, 1);

            MvcResult result = mockMvc.perform(post("/api/orders")
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType("application/json")
                            .content("{\"idempotencyKey\":\"incomplete-profile-"
                                    + UUID.randomUUID()
                                    + "\",\"receiverName\":\"Tom\","
                                    + "\"receiverPhone\":\"13800000000\","
                                    + "\"receiverAddress\":\"Beijing\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals(1013, body(result).path("code").asInt());
        } finally {
            userBuyerService.update(new LambdaUpdateWrapper<UserBuyer>()
                    .eq(UserBuyer::getId, buyer.getId())
                    .set(UserBuyer::getEmail, originalEmail));
        }
    }

    @Test
    void multiSellerCheckoutCreatesIndependentOrders() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        String productName = "拆单测试卖家二商品-" + UUID.randomUUID();
        ProductRequest productRequest = new ProductRequest();
        productRequest.setName(productName);
        productRequest.setDescription("用于验证按卖家拆单");
        productRequest.setPrice(new java.math.BigDecimal("199.00"));
        productRequest.setStock(10);
        productRequest.setCategoryId(1L);
        productRequest.setStatus(1);
        productService.createProduct(2L, productRequest);
        Product secondSellerProduct = productService.lambdaQuery()
                .eq(Product::getName, productName)
                .one();
        assertTrue(secondSellerProduct != null);

        try {
            addToCart(buyerToken, 1, 1);
            addToCart(buyerToken, secondSellerProduct.getId().intValue(), 2);

            JsonNode createdOrders = data(createOrderResult(
                    buyerToken, "multi-seller-" + UUID.randomUUID()));

            assertEquals(2, createdOrders.size());
            assertEquals(2, createdOrders.findValues("sellerId").stream()
                    .map(JsonNode::asLong).distinct().count());
            JsonNode sellerOneOrder = null;
            JsonNode sellerTwoOrder = null;
            for (JsonNode order : createdOrders) {
                if (order.path("sellerId").asLong() == 1L) {
                    sellerOneOrder = order;
                }
                if (order.path("sellerId").asLong() == 2L) {
                    sellerTwoOrder = order;
                }
            }
            assertTrue(sellerOneOrder != null);
            assertTrue(sellerTwoOrder != null);
            assertEquals(5999.00, sellerOneOrder.path("totalAmount").asDouble(), 0.01);
            assertEquals(398.00, sellerTwoOrder.path("totalAmount").asDouble(), 0.01);
            assertEquals("Book House", sellerTwoOrder.path("sellerName").asText());

            String sellerTwoToken = login("SELLER", "seller02", "123456");
            MvcResult sellerTwoPage = mockMvc.perform(get("/api/seller/orders")
                            .header("Authorization", "Bearer " + sellerTwoToken))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode sellerTwoRecords = data(sellerTwoPage).path("records");
            boolean containsSellerTwoOrder = false;
            for (JsonNode order : sellerTwoRecords) {
                if (order.path("id").asLong() == sellerTwoOrder.path("id").asLong()) {
                    containsSellerTwoOrder = true;
                }
            }
            assertTrue(containsSellerTwoOrder);
        } finally {
            productService.deleteProduct(2L, secondSellerProduct.getId());
        }
    }

    @Test
    void orderPagesReturnBatchDetailsWithoutExtraDetailRequests() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        addToCart(buyerToken, 1, 1);
        createOrder(buyerToken);

        MvcResult buyerPage = mockMvc.perform(get("/api/orders/page-details")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode buyerFirst = data(buyerPage).path("records").get(0);
        assertEquals(1, buyerFirst.path("items").size());
        assertEquals("buyer01", buyerFirst.path("buyerUsername").asText());
        assertEquals("Tech Store", buyerFirst.path("shopNames").get(0).asText());

        String sellerToken = login("SELLER", "seller01", "123456");
        MvcResult sellerPage = mockMvc.perform(get("/api/seller/orders/page-details")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode sellerFirst = data(sellerPage).path("records").get(0);
        assertEquals(1L, sellerFirst.path("sellerId").asLong());
        assertEquals(1, sellerFirst.path("items").size());
    }
}
