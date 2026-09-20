package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void adminCanReadAndUpdateOwnProfile() throws Exception {
        String token = login("ADMIN", "admin", "admin");

        MvcResult profileResult = mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode profile = data(profileResult);
        assertEquals("admin", profile.path("username").asText());
        assertEquals("ADMIN", profile.path("role").asText());

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800009999","email":"admin-updated@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode updated = data(result);
                    assertEquals("13800009999", updated.path("phone").asText());
                    assertEquals(
                            "admin-updated@example.com",
                            updated.path("email").asText());
                });
    }
}
