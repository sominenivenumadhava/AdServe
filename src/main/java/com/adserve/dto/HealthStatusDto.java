package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing basic health and metadata information of the AdServe service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {

    private String status;
    private String service;
    private String version;
    private String description;
    private LocalDateTime timestamp;
}
