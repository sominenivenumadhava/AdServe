package com.adserve.controller;

import com.adserve.dto.ApiResponse;
import com.adserve.dto.KafkaStatusDto;
import com.adserve.service.KafkaMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing system health and event-driven infrastructure status.
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final KafkaMetricsService kafkaMetricsService;

    /**
     * Returns real-time health and metrics for Apache Kafka event streaming.
     * Example: GET /api/system/kafka-status
     */
    @GetMapping("/kafka-status")
    public ResponseEntity<ApiResponse<KafkaStatusDto>> getKafkaStatus() {
        KafkaStatusDto status = kafkaMetricsService.getStatus();
        return ResponseEntity.ok(ApiResponse.success(status, "Kafka streaming status retrieved successfully"));
    }
}
