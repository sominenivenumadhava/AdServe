package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Metric transfer object exposing real-time Redis cache performance statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheMetricsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private long cacheHits;
    private long cacheMisses;
    private long redisErrors;
    private long totalRequests;
    private double hitRatePercentage;
    private String cacheStatus;
}
