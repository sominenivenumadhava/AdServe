package com.adserve.service;

import com.adserve.dto.AdAnalyticsDto;
import com.adserve.dto.AdRequestDto;
import com.adserve.dto.AdResponseDto;
import com.adserve.entity.*;
import com.adserve.exception.NoEligibleAdException;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.AdClickRepository;
import com.adserve.repository.AdImpressionRepository;
import com.adserve.repository.AdvertisementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdServingEngineTest {

    @Nested
    @DisplayName("1-6. Ad Serving & Filtering Tests")
    class AdServingTests {

        @Mock
        private AdvertisementRepository advertisementRepository;

        @Spy
        private DeterministicAdSelectionService adSelectionService;

        @Mock
        private ImpressionService impressionService;

        @Mock
        private AdCacheService adCacheService;

        @InjectMocks
        private AdServingService adServingService;

        private Campaign activeCampaign;
        private Advertisement activeAd;
        private AdRequestDto validRequest;

        @BeforeEach
        void setUp() {
            LocalDateTime now = LocalDateTime.now();

            activeCampaign = Campaign.builder()
                    .id(20L)
                    .name("Gaming Blitz")
                    .budget(BigDecimal.valueOf(5000.00))
                    .startDate(now.minusDays(5))
                    .endDate(now.plusDays(25))
                    .status(CampaignStatus.ACTIVE)
                    .targetCountry("IN")
                    .targetDevice("ANDROID")
                    .targetCategory("GAMING")
                    .build();

            activeAd = Advertisement.builder()
                    .id(101L)
                    .campaign(activeCampaign)
                    .title("Gaming Laptop Pro")
                    .imageUrl("https://example.com/laptop.jpg")
                    .targetUrl("https://example.com/buy")
                    .status(AdStatus.ACTIVE)
                    .build();

            validRequest = AdRequestDto.builder()
                    .country("IN")
                    .device("ANDROID")
                    .category("GAMING")
                    .build();
        }

        @Test
        @DisplayName("1. Active campaign selection succeeds when all criteria match")
        void testActiveCampaignSelection() {
            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(List.of(activeAd));

            AdResponseDto response = adServingService.serveAd(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAdId()).isEqualTo(101L);
            assertThat(response.getCampaignId()).isEqualTo(20L);
            assertThat(response.getTitle()).isEqualTo("Gaming Laptop Pro");

            // Verify impression was recorded automatically upon serving
            verify(impressionService, times(1)).recordImpression(101L);
        }

        @Test
        @DisplayName("2. Expired campaign rejection when end date is in the past")
        void testExpiredCampaignRejection() {
            LocalDateTime past = LocalDateTime.now().minusDays(10);
            activeCampaign.setStartDate(past.minusDays(20));
            activeCampaign.setEndDate(past); // Expired

            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(List.of(activeAd));

            assertThatThrownBy(() -> adServingService.serveAd(validRequest))
                    .isInstanceOf(NoEligibleAdException.class)
                    .hasMessageContaining("No eligible advertisement found");

            verify(impressionService, never()).recordImpression(anyLong());
        }

        @Test
        @DisplayName("3. Country mismatch rejection")
        void testCountryMismatchRejection() {
            activeCampaign.setTargetCountry("US"); // Targets US, request is IN

            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(List.of(activeAd));

            assertThatThrownBy(() -> adServingService.serveAd(validRequest))
                    .isInstanceOf(NoEligibleAdException.class)
                    .hasMessageContaining("No eligible advertisement found");

            verify(impressionService, never()).recordImpression(anyLong());
        }

        @Test
        @DisplayName("4. Device mismatch rejection")
        void testDeviceMismatchRejection() {
            activeCampaign.setTargetDevice("DESKTOP"); // Targets DESKTOP, request is ANDROID

            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(List.of(activeAd));

            assertThatThrownBy(() -> adServingService.serveAd(validRequest))
                    .isInstanceOf(NoEligibleAdException.class)
                    .hasMessageContaining("No eligible advertisement found");

            verify(impressionService, never()).recordImpression(anyLong());
        }

        @Test
        @DisplayName("5. Category mismatch rejection")
        void testCategoryMismatchRejection() {
            activeCampaign.setTargetCategory("FINANCE"); // Targets FINANCE, request is GAMING

            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(List.of(activeAd));

            assertThatThrownBy(() -> adServingService.serveAd(validRequest))
                    .isInstanceOf(NoEligibleAdException.class)
                    .hasMessageContaining("No eligible advertisement found");

            verify(impressionService, never()).recordImpression(anyLong());
        }

        @Test
        @DisplayName("6. No eligible advertisement when candidate list is empty")
        void testNoEligibleAdvertisement() {
            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> adServingService.serveAd(validRequest))
                    .isInstanceOf(NoEligibleAdException.class)
                    .hasMessageContaining("No eligible advertisement found");

            verify(impressionService, never()).recordImpression(anyLong());
        }

        @Test
        @DisplayName("Cache HIT: Uses Redis cached candidates and skips MySQL query")
        void testServeAdCacheHit() {
            com.adserve.dto.CachedAdDto cachedDto = com.adserve.dto.CachedAdDto.fromEntity(activeAd);
            when(adCacheService.getEligibleAds("IN", "ANDROID", "GAMING"))
                    .thenReturn(Optional.of(List.of(cachedDto)));

            AdResponseDto response = adServingService.serveAd(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAdId()).isEqualTo(101L);
            assertThat(response.getTitle()).isEqualTo("Gaming Laptop Pro");

            // Database should NOT be queried when cache hits
            verify(advertisementRepository, never()).findAllWithCampaignByStatus(any());
            // Impression should still be recorded
            verify(impressionService, times(1)).recordImpression(101L);
        }

        @Test
        @DisplayName("Cache MISS: Queries MySQL and populates Redis cache")
        void testServeAdCacheMissPopulatesCache() {
            when(adCacheService.getEligibleAds("IN", "ANDROID", "GAMING"))
                    .thenReturn(Optional.empty());
            when(advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE))
                    .thenReturn(List.of(activeAd));

            AdResponseDto response = adServingService.serveAd(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAdId()).isEqualTo(101L);

            // Database should be queried on cache miss
            verify(advertisementRepository, times(1)).findAllWithCampaignByStatus(AdStatus.ACTIVE);
            // Cache should be populated with eligible candidates
            verify(adCacheService, times(1)).cacheEligibleAds(
                    eq("IN"), eq("ANDROID"), eq("GAMING"), anyList());
        }
    }

    @Nested
    @DisplayName("7. Impression Recording Tests")
    class ImpressionRecordingTests {

        @Mock
        private AdImpressionRepository adImpressionRepository;

        @InjectMocks
        private ImpressionService impressionService;

        @Test
        @DisplayName("7. Successfully records an impression in MySQL")
        void testImpressionRecording() {
            AdImpression savedImpression = AdImpression.builder()
                    .id(1L)
                    .adId(101L)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(adImpressionRepository.save(any(AdImpression.class))).thenReturn(savedImpression);

            AdImpression result = impressionService.recordImpression(101L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAdId()).isEqualTo(101L);
            verify(adImpressionRepository, times(1)).save(any(AdImpression.class));
        }
    }

    @Nested
    @DisplayName("8. Click Recording Tests")
    class ClickRecordingTests {

        @Mock
        private AdClickRepository adClickRepository;

        @Mock
        private AdvertisementRepository advertisementRepository;

        @InjectMocks
        private ClickService clickService;

        @Test
        @DisplayName("8. Successfully records a click in MySQL when ad exists")
        void testClickRecordingSuccess() {
            when(advertisementRepository.existsById(101L)).thenReturn(true);

            AdClick savedClick = AdClick.builder()
                    .id(1L)
                    .adId(101L)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(adClickRepository.save(any(AdClick.class))).thenReturn(savedClick);

            AdClick result = clickService.recordClick(101L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAdId()).isEqualTo(101L);
            verify(adClickRepository, times(1)).save(any(AdClick.class));
        }

        @Test
        @DisplayName("8b. Throws ResourceNotFoundException when ad does not exist")
        void testClickRecordingAdNotFound() {
            when(advertisementRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> clickService.recordClick(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Advertisement not found");

            verify(adClickRepository, never()).save(any(AdClick.class));
        }
    }

    @Nested
    @DisplayName("9. CTR Calculation Tests")
    class CtrCalculationTests {

        @Mock
        private ImpressionService impressionService;

        @Mock
        private ClickService clickService;

        @Mock
        private AdvertisementRepository advertisementRepository;

        @InjectMocks
        private AnalyticsService analyticsService;

        @Test
        @DisplayName("9. Calculates standard CTR correctly (450 clicks / 10000 impressions = 4.5%)")
        void testStandardCtrCalculation() {
            when(advertisementRepository.existsById(101L)).thenReturn(true);
            when(impressionService.getImpressionCount(101L)).thenReturn(10000L);
            when(clickService.getClickCount(101L)).thenReturn(450L);

            AdAnalyticsDto analytics = analyticsService.getAnalytics(101L);

            assertThat(analytics).isNotNull();
            assertThat(analytics.getAdId()).isEqualTo(101L);
            assertThat(analytics.getImpressions()).isEqualTo(10000L);
            assertThat(analytics.getClicks()).isEqualTo(450L);
            assertThat(analytics.getCtr()).isEqualTo(4.5);
        }

        @Test
        @DisplayName("9b. Safely handles zero impressions without division by zero (CTR = 0.0%)")
        void testZeroImpressionsCtrCalculation() {
            when(advertisementRepository.existsById(101L)).thenReturn(true);
            when(impressionService.getImpressionCount(101L)).thenReturn(0L);
            when(clickService.getClickCount(101L)).thenReturn(0L);

            AdAnalyticsDto analytics = analyticsService.getAnalytics(101L);

            assertThat(analytics).isNotNull();
            assertThat(analytics.getImpressions()).isEqualTo(0L);
            assertThat(analytics.getCtr()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("9c. Correctly rounds CTR to 2 decimal places (1 click / 3 impressions = 33.33%)")
        void testRoundedCtrCalculation() {
            double ctr = analyticsService.calculateCtr(1, 3);
            assertThat(ctr).isEqualTo(33.33);
        }
    }
}
