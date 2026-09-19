package com.adserve.service;

import com.adserve.dto.CacheMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheMetricsServiceTest {

    private CacheMetricsService cacheMetricsService;

    @BeforeEach
    void setUp() {
        cacheMetricsService = new CacheMetricsService(null);
    }

    @Test
    @DisplayName("Initial metrics are zero and hit rate is 0.0%")
    void testInitialMetrics() {
        CacheMetricsDto metrics = cacheMetricsService.getMetrics(true);

        assertThat(metrics.getCacheHits()).isEqualTo(0);
        assertThat(metrics.getCacheMisses()).isEqualTo(0);
        assertThat(metrics.getRedisErrors()).isEqualTo(0);
        assertThat(metrics.getTotalRequests()).isEqualTo(0);
        assertThat(metrics.getHitRatePercentage()).isEqualTo(0.0);
        assertThat(metrics.getCacheStatus()).isEqualTo("ONLINE");
    }

    @Test
    @DisplayName("Hit rate calculation correctly computes 85.0% for 850 hits and 150 misses")
    void testHitRateCalculation() {
        for (int i = 0; i < 850; i++) {
            cacheMetricsService.recordHit();
        }
        for (int i = 0; i < 150; i++) {
            cacheMetricsService.recordMiss();
        }

        CacheMetricsDto metrics = cacheMetricsService.getMetrics(true);

        assertThat(metrics.getCacheHits()).isEqualTo(850);
        assertThat(metrics.getCacheMisses()).isEqualTo(150);
        assertThat(metrics.getTotalRequests()).isEqualTo(1000);
        assertThat(metrics.getHitRatePercentage()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Redis error counter increments independently")
    void testRecordRedisError() {
        cacheMetricsService.recordRedisError();
        cacheMetricsService.recordRedisError();

        CacheMetricsDto metrics = cacheMetricsService.getMetrics(false);

        assertThat(metrics.getRedisErrors()).isEqualTo(2);
        assertThat(metrics.getCacheStatus()).isEqualTo("OFFLINE");
    }

    @Test
    @DisplayName("Reset clears all counters")
    void testReset() {
        cacheMetricsService.recordHit();
        cacheMetricsService.recordMiss();
        cacheMetricsService.recordRedisError();

        cacheMetricsService.reset();
        CacheMetricsDto metrics = cacheMetricsService.getMetrics(true);

        assertThat(metrics.getCacheHits()).isEqualTo(0);
        assertThat(metrics.getCacheMisses()).isEqualTo(0);
        assertThat(metrics.getRedisErrors()).isEqualTo(0);
    }
}
