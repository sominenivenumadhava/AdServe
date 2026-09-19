package com.adserve.service;

import com.adserve.dto.CacheMetricsDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service tracking Redis cache performance metrics (hits, misses, errors, hit rate).
 * Thread-safe implementation integrated with Micrometer / Spring Boot Actuator.
 */
@Service
public class CacheMetricsService {

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong redisErrors = new AtomicLong(0);

    private Counter micrometerHits;
    private Counter micrometerMisses;
    private Counter micrometerErrors;

    public CacheMetricsService(@Autowired(required = false) MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            this.micrometerHits = Counter.builder("adserve.cache.hits")
                    .description("Count of successful Redis cache hits")
                    .register(meterRegistry);
            this.micrometerMisses = Counter.builder("adserve.cache.misses")
                    .description("Count of Redis cache misses requiring MySQL lookup")
                    .register(meterRegistry);
            this.micrometerErrors = Counter.builder("adserve.cache.errors")
                    .description("Count of Redis communication or connection errors")
                    .register(meterRegistry);
        }
    }

    /**
     * Increments the cache hit counter.
     */
    public void recordHit() {
        cacheHits.incrementAndGet();
        if (micrometerHits != null) {
            micrometerHits.increment();
        }
    }

    /**
     * Increments the cache miss counter.
     */
    public void recordMiss() {
        cacheMisses.incrementAndGet();
        if (micrometerMisses != null) {
            micrometerMisses.increment();
        }
    }

    /**
     * Increments the Redis communication error counter.
     */
    public void recordRedisError() {
        redisErrors.incrementAndGet();
        if (micrometerErrors != null) {
            micrometerErrors.increment();
        }
    }

    /**
     * Aggregates and returns current cache performance metrics.
     *
     * @param isRedisConnected Current connectivity status indicator
     * @return CacheMetricsDto snapshot
     */
    public CacheMetricsDto getMetrics(boolean isRedisConnected) {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long errors = redisErrors.get();
        long total = hits + misses;

        double hitRate = 0.0;
        if (total > 0) {
            hitRate = BigDecimal.valueOf((double) hits / total * 100.0)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return CacheMetricsDto.builder()
                .cacheHits(hits)
                .cacheMisses(misses)
                .redisErrors(errors)
                .totalRequests(total)
                .hitRatePercentage(hitRate)
                .cacheStatus(isRedisConnected ? "ONLINE" : "OFFLINE")
                .build();
    }

    /**
     * Resets all metric counters (useful for test resets).
     */
    public void reset() {
        cacheHits.set(0);
        cacheMisses.set(0);
        redisErrors.set(0);
    }
}
