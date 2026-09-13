package com.example.project01.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerStatsIntegrationTest extends IntegrationTestSupport {

    @Test
    void sellerCanReadStatistics() throws Exception {
        String sellerToken = login("SELLER", "seller01", "123456");
        MvcResult stats = mockMvc.perform(get("/api/seller/stats?range=all")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(data(stats).get("totalRevenue"));
        assertNotNull(data(stats).get("orderCount"));
    }
}
