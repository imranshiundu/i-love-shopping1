package com.iloveshopping.health;

import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthCheckTest {

    @Test
    void shouldValidateHealthCheckResponseStructure() {
        HealthCheckResponse response = HealthCheckResponse.builder()
                .status("UP")
                .timestamp("2024-01-01T00:00:00Z")
                .uptime(3600.0)
                .environment("test")
                .version("1.0.0")
                .build();

        assertEquals("UP", response.getStatus());
        assertNotNull(response.getTimestamp());
        assertEquals("test", response.getEnvironment());
    }

    @Data
    @Builder
    static class HealthCheckResponse {
        private String status;
        private String timestamp;
        private Double uptime;
        private String environment;
        private String version;
    }
}