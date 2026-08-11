package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Application health endpoints")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "application", "i-love-shopping",
                "version", "1.0.0-SNAPSHOT"
        );
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness probe")
    public ResponseEntity<ApiResponse<String>> live() {
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe")
    public ResponseEntity<ApiResponse<String>> ready() {
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}
