package com.adserve.controller;

import com.adserve.dto.AdAnalyticsDto;
import com.adserve.dto.AdRequestDto;
import com.adserve.dto.AdResponseDto;
import com.adserve.dto.ApiResponse;
import com.adserve.service.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ad serving, impression tracking, click tracking, and performance analytics.
 * Adheres strictly to thin-controller principles: receives input, validates, delegates to services, and returns responses.
 */
@RestController
@RequestMapping("/api/ad-server")
@RequiredArgsConstructor
@Validated
public class AdServerController {

    private final AdServingService adServingService;
    private final ImpressionService impressionService;
    private final ClickService clickService;
    private final AnalyticsService analyticsService;
    private final CacheMetricsService cacheMetricsService;
    private final AdCacheService adCacheService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AdEventProducer adEventProducer;

    /**
     * Serves an advertisement based on incoming targeting criteria.
     * Example: GET /api/ad-server/serve?country=IN&device=ANDROID&category=GAMING
     */
    @GetMapping("/serve")
    public ResponseEntity<ApiResponse<AdResponseDto>> serveAd(
            @RequestParam @NotBlank(message = "Country parameter is required") String country,
            @RequestParam @NotBlank(message = "Device parameter is required") String device,
            @RequestParam @NotBlank(message = "Category parameter is required") String category) {

        AdRequestDto requestDto = AdRequestDto.builder()
                .country(country)
                .device(device)
                .category(category)
                .build();

        AdResponseDto responseDto = adServingService.serveAd(requestDto);
        return ResponseEntity.ok(ApiResponse.success(responseDto, "Ad served successfully"));
    }

    /**
     * Records an impression beacon for an advertisement.
     * Dispatches AdImpressionEvent to Kafka for asynchronous consumer persistence.
     * Example: POST /api/ad-server/101/impression
     */
    @PostMapping("/{id}/impression")
    public ResponseEntity<ApiResponse<String>> recordImpression(
            @PathVariable("id") @NotNull(message = "Ad ID is required") Long id) {
        if (adEventProducer != null) {
            adEventProducer.publishImpression(com.adserve.event.AdImpressionEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .adId(id)
                    .timestamp(java.time.LocalDateTime.now())
                    .build());
        } else {
            impressionService.recordImpression(id);
        }
        return ResponseEntity.ok(ApiResponse.success("Impression recorded successfully", "Impression recorded successfully"));
    }

    /**
     * Records a click beacon for an advertisement.
     * Dispatches AdClickEvent to Kafka for asynchronous consumer persistence.
     * Example: POST /api/ad-server/101/click
     */
    @PostMapping("/{id}/click")
    public ResponseEntity<ApiResponse<String>> recordClick(
            @PathVariable("id") @NotNull(message = "Ad ID is required") Long id) {
        if (adEventProducer != null) {
            adEventProducer.publishClick(com.adserve.event.AdClickEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .adId(id)
                    .timestamp(java.time.LocalDateTime.now())
                    .build());
        } else {
            clickService.recordClick(id);
        }
        return ResponseEntity.ok(ApiResponse.success("Click recorded successfully", "Click recorded successfully"));
    }

    /**
     * Returns real-time CTR analytics for a specific advertisement.
     * Example: GET /api/ad-server/101/analytics
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<ApiResponse<AdAnalyticsDto>> getAdAnalytics(
            @PathVariable("id") @NotNull(message = "Ad ID is required") Long id) {
        AdAnalyticsDto analytics = analyticsService.getAnalytics(id);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Ad analytics retrieved successfully"));
    }

    /**
     * Returns aggregated top-level dashboard metrics (totals and overall average CTR).
     */
    @GetMapping("/analytics/overview")
    public ResponseEntity<ApiResponse<com.adserve.dto.DashboardOverviewDto>> getDashboardOverview() {
        com.adserve.dto.DashboardOverviewDto overview = analyticsService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse.success(overview, "Dashboard overview retrieved successfully"));
    }

    /**
     * Returns campaign-level analytics breakdown for charts and reporting tables.
     */
    @GetMapping("/analytics/campaigns")
    public ResponseEntity<ApiResponse<java.util.List<com.adserve.dto.CampaignAnalyticsDto>>> getCampaignAnalytics() {
        java.util.List<com.adserve.dto.CampaignAnalyticsDto> campaignAnalytics = analyticsService.getCampaignAnalytics();
        return ResponseEntity.ok(ApiResponse.success(campaignAnalytics, "Campaign analytics retrieved successfully"));
    }

    /**
     * Returns real-time Redis cache performance metrics (hits, misses, errors, hit rate).
     * Example: GET /api/ad-server/cache/metrics
     */
    @GetMapping("/cache/metrics")
    public ResponseEntity<ApiResponse<com.adserve.dto.CacheMetricsDto>> getCacheMetrics() {
        com.adserve.dto.CacheMetricsDto metrics = cacheMetricsService.getMetrics(adCacheService.isRedisAvailable());
        return ResponseEntity.ok(ApiResponse.success(metrics, "Cache metrics retrieved successfully"));
    }

    /**
     * Admin endpoint to manually flush all eligible ad caches in Redis.
     * Example: POST /api/ad-server/cache/clear
     */
    @PostMapping("/cache/clear")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearCache() {
        adCacheService.clearRelevantCache();
        return ResponseEntity.ok(ApiResponse.success("Cache cleared successfully", "All eligible ad caches cleared successfully"));
    }
}
