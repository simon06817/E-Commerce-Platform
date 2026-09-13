package com.example.project01.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    void wrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"BUYER\",\"username\":\"buyer01\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertEquals(1006, body.path("code").asInt());
                });
    }

    @Test
    void registerRefreshAndLogoutFlow() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"BUYER\",\"username\":\"newbuyer01\","
                                + "\"password\":\"123456\",\"nickname\":\"new buyer\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode registered = data(registerResult);
        String accessToken = registered.path("token").asText();
        String refreshToken = registered.path("refreshToken").asText();
        assertFalse(accessToken.isBlank());
        assertFalse(refreshToken.isBlank());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshed = data(refreshResult);
        String newAccessToken = refreshed.path("token").asText();
        String newRefreshToken = refreshed.path("refreshToken").asText();
        assertFalse(refreshToken.equals(newRefreshToken));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileUpdateAndPasswordChange() throws Exception {
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"BUYER\",\"username\":\"profilebuyer\","
                                + "\"password\":\"123456\",\"nickname\":\"profile user\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = data(registered).path("token").asText();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"profile updated\",\"phone\":\"13800009999\","
                                + "\"email\":\"profile@example.com\",\"address\":\"Shanghai\"}"))
                .andExpect(status().isOk());

        MvcResult profile = mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("profile updated", data(profile).path("nickname").asText());
        assertEquals("Shanghai", data(profile).path("address").asText());

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk());

        assertFalse(login("BUYER", "profilebuyer", "654321").isBlank());
    }
}
