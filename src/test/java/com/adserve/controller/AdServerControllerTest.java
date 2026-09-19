package com.adserve.controller;

import com.adserve.config.AppConfig;
import com.adserve.dto.AdAnalyticsDto;
import com.adserve.dto.AdRequestDto;
import com.adserve.dto.AdResponseDto;
import com.adserve.entity.AdClick;
import com.adserve.entity.AdImpression;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.exception.NoEligibleAdException;
import com.adserve.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdServerController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import({AppConfig.class, GlobalExceptionHandler.class})
class AdServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdServingService adServingService;

    @MockBean
    private ImpressionService impressionService;

    @MockBean
    private ClickService clickService;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private CacheMetricsService cacheMetricsService;

    @MockBean
    private AdCacheService adCacheService;

    @MockBean
    private AdEventProducer adEventProducer;

    @Test
    @DisplayName("GET /api/ad-server/serve returns 200 and AdResponseDto when an eligible ad is found")
    void testServeAdSuccess() throws Exception {
        AdResponseDto responseDto = AdResponseDto.builder()
                .adId(101L)
                .campaignId(20L)
                .title("Gaming Laptop")
                .imageUrl("https://example.com/ad.jpg")
                .targetUrl("https://example.com/product")
                .build();

        when(adServingService.serveAd(any(AdRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(get("/api/ad-server/serve")
                        .param("country", "IN")
                        .param("device", "ANDROID")
                        .param("category", "GAMING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.adId", is(101)))
                .andExpect(jsonPath("$.data.campaignId", is(20)))
                .andExpect(jsonPath("$.data.title", is("Gaming Laptop")))
                .andExpect(jsonPath("$.data.imageUrl", is("https://example.com/ad.jpg")))
                .andExpect(jsonPath("$.data.targetUrl", is("https://example.com/product")));
    }

    @Test
    @DisplayName("GET /api/ad-server/serve returns 404 when no eligible ad is found")
    void testServeAdNoEligibleAd() throws Exception {
        when(adServingService.serveAd(any(AdRequestDto.class)))
                .thenThrow(new NoEligibleAdException("IN", "ANDROID", "GAMING"));

        mockMvc.perform(get("/api/ad-server/serve")
                        .param("country", "IN")
                        .param("device", "ANDROID")
                        .param("category", "GAMING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("No Eligible Ad")));
    }

    @Test
    @DisplayName("POST /api/ad-server/{adId}/impression dispatches Kafka event and returns 200")
    void testRecordImpression() throws Exception {
        mockMvc.perform(post("/api/ad-server/101/impression")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Impression recorded successfully")));

        org.mockito.Mockito.verify(adEventProducer, org.mockito.Mockito.times(1))
                .publishImpression(org.mockito.ArgumentMatchers.any(com.adserve.event.AdImpressionEvent.class));
    }

    @Test
    @DisplayName("POST /api/ad-server/{adId}/click dispatches Kafka event and returns 200")
    void testRecordClick() throws Exception {
        mockMvc.perform(post("/api/ad-server/101/click")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Click recorded successfully")));

        org.mockito.Mockito.verify(adEventProducer, org.mockito.Mockito.times(1))
                .publishClick(org.mockito.ArgumentMatchers.any(com.adserve.event.AdClickEvent.class));
    }

    @Test
    @DisplayName("GET /api/ad-server/{adId}/analytics returns impression, click counts, and CTR")
    void testGetAnalytics() throws Exception {
        AdAnalyticsDto analyticsDto = AdAnalyticsDto.builder()
                .adId(101L)
                .impressions(10000L)
                .clicks(450L)
                .ctr(4.5)
                .build();

        when(analyticsService.getAnalytics(eq(101L))).thenReturn(analyticsDto);

        mockMvc.perform(get("/api/ad-server/101/analytics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.adId", is(101)))
                .andExpect(jsonPath("$.data.impressions", is(10000)))
                .andExpect(jsonPath("$.data.clicks", is(450)))
                .andExpect(jsonPath("$.data.ctr", is(4.5)));
    }

    @Test
    @DisplayName("GET /api/ad-server/cache/metrics returns cache hit rate and counters")
    void testGetCacheMetrics() throws Exception {
        com.adserve.dto.CacheMetricsDto metricsDto = com.adserve.dto.CacheMetricsDto.builder()
                .cacheHits(850)
                .cacheMisses(150)
                .redisErrors(0)
                .totalRequests(1000)
                .hitRatePercentage(85.0)
                .cacheStatus("ONLINE")
                .build();

        when(cacheMetricsService.getMetrics(anyBoolean())).thenReturn(metricsDto);

        mockMvc.perform(get("/api/ad-server/cache/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.cacheHits", is(850)))
                .andExpect(jsonPath("$.data.cacheMisses", is(150)))
                .andExpect(jsonPath("$.data.hitRatePercentage", is(85.0)))
                .andExpect(jsonPath("$.data.cacheStatus", is("ONLINE")));
    }

    @Test
    @DisplayName("POST /api/ad-server/cache/clear flushes cache and returns success message")
    void testClearCache() throws Exception {
        mockMvc.perform(post("/api/ad-server/cache/clear")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("All eligible ad caches cleared successfully")));
    }
}

