package com.example.project01.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityIntegrationTest extends IntegrationTestSupport {

    @Test
    void healthEndpointReturnsStatusAndTraceId() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().string("X-Trace-Id", not(blankOrNullString())));
    }

    @Test
    void incomingTraceIdIsReusedAndPrometheusMetricsAreExposed() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Trace-Id", "test-trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-123"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "jvm_memory_used_bytes")));
    }
}
