package com.adserve.service;

import com.adserve.dto.CachedAdDto;
import com.adserve.entity.AdStatus;
import com.adserve.entity.Campaign;
import com.adserve.entity.CampaignStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private CacheMetricsService cacheMetricsService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdCacheService adCacheService;

    private CachedAdDto sampleAdDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adCacheService, "ttlSeconds", 300L);

        sampleAdDto = CachedAdDto.builder()
                .adId(101L)
                .campaignId(10L)
                .title("Gaming Laptop Pro")
                .imageUrl("https://example.com/laptop.jpg")
                .targetUrl("https://example.com/buy")
                .budget(new BigDecimal("5000.00"))
                .targetCountry("IN")
                .targetDevice("ANDROID")
                .targetCategory("GAMING")
                .status(AdStatus.ACTIVE)
                .campaignStatus(CampaignStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Cache HIT: Returns cached eligible ads and records hit metric")
    void testGetEligibleAdsHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("adserve:eligible:IN:ANDROID:GAMING"))
                .thenReturn(Collections.singletonList(sampleAdDto));

        Optional<List<CachedAdDto>> result = adCacheService.getEligibleAds("IN", "ANDROID", "GAMING");

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0).getTitle()).isEqualTo("Gaming Laptop Pro");

        verify(cacheMetricsService, times(1)).recordHit();
        verify(cacheMetricsService, never()).recordMiss();
    }

    @Test
    @DisplayName("Cache MISS: Returns empty and records miss metric")
    void testGetEligibleAdsMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("adserve:eligible:IN:ANDROID:GAMING")).thenReturn(null);

        Optional<List<CachedAdDto>> result = adCacheService.getEligibleAds("IN", "ANDROID", "GAMING");

        assertThat(result).isEmpty();
        verify(cacheMetricsService, times(1)).recordMiss();
        verify(cacheMetricsService, never()).recordHit();
    }

    @Test
    @DisplayName("Cache Set: Stores eligible ads with configured 300s TTL")
    void testCacheEligibleAds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adCacheService.cacheEligibleAds("IN", "ANDROID", "GAMING", Collections.singletonList(sampleAdDto));

        verify(valueOperations, times(1)).set(
                eq("adserve:eligible:IN:ANDROID:GAMING"),
                eq(Collections.singletonList(sampleAdDto)),
                eq(Duration.ofSeconds(300L))
        );
    }

    @Test
    @DisplayName("Redis Failure on Read: Gracefully logs warning, records error, and returns empty without failing")
    void testGetEligibleAdsRedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection refused to Redis at localhost:6379"));

        Optional<List<CachedAdDto>> result = adCacheService.getEligibleAds("IN", "ANDROID", "GAMING");

        assertThat(result).isEmpty();
        verify(cacheMetricsService, times(1)).recordRedisError();
        verify(cacheMetricsService, times(1)).recordMiss();
    }

    @Test
    @DisplayName("Redis Failure on Write: Gracefully logs warning, records error, and does not throw exception")
    void testCacheEligibleAdsRedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Redis timeout"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        adCacheService.cacheEligibleAds("IN", "ANDROID", "GAMING", Collections.singletonList(sampleAdDto));

        verify(cacheMetricsService, times(1)).recordRedisError();
    }

    @Test
    @DisplayName("Cache Invalidation: Evicts key for campaign targeting")
    void testEvictCampaignCache() {
        Campaign campaign = Campaign.builder()
                .id(1L)
                .targetCountry("US")
                .targetDevice("MOBILE")
                .targetCategory("SPORTS")
                .build();

        adCacheService.evictCampaignCache(campaign);

        verify(redisTemplate, times(1)).delete("adserve:eligible:US:MOBILE:SPORTS");
    }

    @Test
    @DisplayName("Cache Invalidation: Clears all eligible ad keys matching pattern")
    void testEvictAllEligibleAds() {
        Set<String> keys = new HashSet<>(Arrays.asList(
                "adserve:eligible:US:MOBILE:SPORTS",
                "adserve:eligible:IN:ANDROID:GAMING"
        ));
        when(redisTemplate.keys("adserve:eligible:*")).thenReturn(keys);

        adCacheService.evictAllEligibleAds();

        verify(redisTemplate, times(1)).delete(keys);
    }

    @Test
    @DisplayName("isRedisAvailable: Returns true when PING succeeds")
    void testIsRedisAvailableSuccess() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        boolean available = adCacheService.isRedisAvailable();

        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("isRedisAvailable: Returns false when connection fails")
    void testIsRedisAvailableFailure() {
        when(redisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("No connection"));

        boolean available = adCacheService.isRedisAvailable();

        assertThat(available).isFalse();
    }
}
