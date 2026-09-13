package com.example.project01.integration;

import com.example.project01.service.OrderNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderNotificationService notificationService;

    @Test
    void notificationCreationIsIdempotentAndCanBeMarkedRead() throws Exception {
        String buyerToken = login("BUYER", "buyer02", "123456");
        Long buyerId = data(mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + buyerToken))
                .andReturn()).path("id").asLong();

        notificationService.createIfAbsent(999999L, "TEST_NOTIFICATION", buyerId, "BUYER",
                "test title", "test content");
        notificationService.createIfAbsent(999999L, "TEST_NOTIFICATION", buyerId, "BUYER",
                "test title", "test content");

        MvcResult notifications = mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andReturn();

        int matches = 0;
        long notificationId = -1;
        for (JsonNode record : data(notifications).path("records")) {
            if ("TEST_NOTIFICATION".equals(record.path("eventType").asText())) {
                matches++;
                notificationId = record.path("id").asLong();
            }
        }
        assertEquals(1, matches);
        assertTrue(notificationId > 0);

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
    }
}
