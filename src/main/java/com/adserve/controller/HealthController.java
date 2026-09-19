package com.adserve.controller;

import com.adserve.dto.ApiResponse;
import com.adserve.dto.HealthStatusDto;
import com.adserve.exception.BadRequestException;
import com.adserve.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Controller exposing health and diagnostic endpoints for application monitoring and verification.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * Primary application health check endpoint.
     *
     * @return Standard ApiResponse wrapping HealthStatusDto
     */
    @GetMapping
    public ResponseEntity<ApiResponse<HealthStatusDto>> checkHealth() {
        HealthStatusDto healthStatus = HealthStatusDto.builder()
                .status("UP")
                .service("AdServe - Mini Ad Serving & Campaign Analytics Platform")
                .version("0.0.1-SNAPSHOT")
                .description("Phase 1: Backend Foundation successfully established.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(healthStatus, "Service is healthy and operating normally"));
    }

    /**
     * Lightweight ping endpoint for quick uptime tests.
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong", "Ping successful"));
    }

    /**
     * Diagnostic endpoint demonstrating global exception handling.
     * Can trigger deliberate exceptions based on the 'type' parameter.
     */
    @GetMapping("/test-error")
    public ResponseEntity<Void> testError(@RequestParam(defaultValue = "bad_request") String type) {
        if ("not_found".equalsIgnoreCase(type)) {
            throw new ResourceNotFoundException("Diagnostic resource not found for testing 404 response");
        } else if ("bad_request".equalsIgnoreCase(type)) {
            throw new BadRequestException("Simulated bad request to verify ErrorResponse structure");
        } else {
            throw new RuntimeException("Simulated unhandled exception to verify 500 error handling");
        }
    }
}
