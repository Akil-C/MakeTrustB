package com.markettrust.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public Root and Health Check Controller.
 * Provides instant HTTP 200 responses for cloud platform health probes (Render, Railway, AWS ECS, GCP Cloud Run)
 * and root endpoint status checks.
 */
@RestController
@Tag(name = "Health & Status", description = "Public endpoints for deployment monitoring and health probes")
public class RootController {

    @GetMapping({"/", "/health", "/api/health"})
    @Operation(summary = "Cloud Health & Status Check", description = "Returns system UP status for cloud deployment verification")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "MarketTrust Backend API",
                "version", "1.0.0",
                "timestamp", Instant.now().toString(),
                "docs", "/swagger-ui.html"
        ));
    }
}
