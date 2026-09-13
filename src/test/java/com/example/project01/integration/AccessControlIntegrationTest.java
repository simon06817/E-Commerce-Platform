package com.example.project01.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlIntegrationTest extends IntegrationTestSupport {

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
}
