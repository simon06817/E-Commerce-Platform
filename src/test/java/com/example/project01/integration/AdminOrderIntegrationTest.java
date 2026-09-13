package com.example.project01.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOrderIntegrationTest extends IntegrationTestSupport {

    @Test
    void adminCanForceCancelOrderAndRestoreStock() throws Exception {
        String buyerToken = login("BUYER", "buyer02", "123456");
        String adminToken = login("ADMIN", "admin", "admin");
        int stockBefore = productStock(3);

        addToCart(buyerToken, 3, 1);
        long orderId = createOrder(buyerToken);

        mockMvc.perform(put("/api/admin/orders/" + orderId + "/force-cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertEquals(4, orderDetail(buyerToken, orderId).path("status").asInt());
        assertEquals(stockBefore, productStock(3));
    }
}
