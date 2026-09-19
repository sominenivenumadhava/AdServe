package com.adserve.service;

import com.adserve.dto.CachedAdDto;
import com.adserve.entity.Campaign;
import com.adserve.util.AdCacheKeyUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Dedicated cache service managing ad-serving Redis operations using the Cache-Aside pattern.
 * Encapsulates key generation, TTL management, cache invalidation, and graceful error fallback.
 */
@Service
@RequiredArgsConstructor
public class AdCacheService {

    private static final Logger log = LoggerFactory.getLogger(AdCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMetricsService cacheMetricsService;
    private final ObjectMapper objectMapper;

    @Value("${adserve.cache.ttl-seconds:300}")
    private long ttlSeconds;

    /**
     * Looks up eligible advertisements from Redis by targeting parameters.
     *
     * @param country Target country
     * @param device Target device
     * @param category Target category
     * @return Optional containing cached candidate list on HIT, or empty on MISS/error
     */
    public Optional<List<CachedAdDto>> getEligibleAds(String country, String device, String category) {
        String key = AdCacheKeyUtil.buildEligibleAdsKey(country, device, category);

        try {
            Object rawCached = redisTemplate.opsForValue().get(key);

            if (rawCached != null) {
                log.info("Redis cache HIT: {}", key);
                cacheMetricsService.recordHit();
                List<CachedAdDto> candidates = deserializeCandidateList(rawCached);
                return Optional.of(candidates);
            } else {
                log.info("Redis cache MISS: {}", key);
                cacheMetricsService.recordMiss();
                return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to MySQL: {}", e.getMessage());
            cacheMetricsService.recordRedisError();
            cacheMetricsService.recordMiss();
            return Optional.empty();
        }
    }

    /**
     * Stores eligible advertisements in Redis under the targeting key with configured TTL.
     *
     * @param country Target country
     * @param device Target device
     * @param category Target category
     * @param ads Eligible ad DTO list
     */
    public void cacheEligibleAds(String country, String device, String category, List<CachedAdDto> ads) {
        String key = AdCacheKeyUtil.buildEligibleAdsKey(country, device, category);

        try {
            redisTemplate.opsForValue().set(key, ads, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached {} eligible ads in Redis for key: {} (TTL: {}s)", ads.size(), key, ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.getMessage());
            cacheMetricsService.recordRedisError();
        }
    }

    /**
     * Evicts a specific cache key.
     *
     * @param key Exact Redis key to delete
     */
    public void evictCache(String key) {
        if (key == null) {
            return;
        }

        try {
            redisTemplate.delete(key);
            log.info("Evicted Redis cache key: {}", key);
        } catch (Exception e) {
            log.warn("Redis eviction failed for key {}: {}", key, e.getMessage());
            cacheMetricsService.recordRedisError();
        }
    }

    /**
     * Evicts cache entries corresponding to specific targeting criteria.
     */
    public void evictByTargeting(String country, String device, String category) {
        String key = AdCacheKeyUtil.buildEligibleAdsKey(country, device, category);
        evictCache(key);
    }

    /**
     * Evicts cached eligible ads for a given campaign based on its targeting fields.
     */
    public void evictCampaignCache(Campaign campaign) {
        if (campaign == null) {
            return;
        }
        evictByTargeting(campaign.getTargetCountry(), campaign.getTargetDevice(), campaign.getTargetCategory());
    }

    /**
     * Clears all eligible ad-serving caches matching the global pattern.
     */
    public void clearRelevantCache() {
        evictAllEligibleAds();
    }

    /**
     * Evicts all eligible ad cache keys (adserve:eligible:*).
     */
    public void evictAllEligibleAds() {
        try {
            Set<String> keys = redisTemplate.keys(AdCacheKeyUtil.GLOBAL_PATTERN);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} eligible ad cache keys from Redis", keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis flush of eligible ads failed: {}", e.getMessage());
            cacheMetricsService.recordRedisError();
        }
    }

    /**
     * Checks if Redis connection is currently responsive.
     */
    public boolean isRedisAvailable() {
        try {
            if (redisTemplate.getConnectionFactory() == null) {
                return false;
            }
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Safely deserializes the raw cached object into a typed List of CachedAdDto.
     */
    @SuppressWarnings("unchecked")
    private List<CachedAdDto> deserializeCandidateList(Object raw) {
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            if (list.get(0) instanceof CachedAdDto) {
                return (List<CachedAdDto>) list;
            }
            return objectMapper.convertValue(raw, new TypeReference<List<CachedAdDto>>() {});
        }
        return Collections.emptyList();
    }
}
