package com.adserve.service;

import com.adserve.dto.AdRequestDto;
import com.adserve.dto.AdResponseDto;
import com.adserve.dto.CachedAdDto;
import com.adserve.entity.AdStatus;
import com.adserve.entity.Advertisement;
import com.adserve.entity.Campaign;
import com.adserve.entity.CampaignStatus;
import com.adserve.exception.NoEligibleAdException;
import com.adserve.repository.AdvertisementRepository;
import com.adserve.event.AdImpressionEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core ad serving orchestration service.
 * Filters active advertisements, verifies campaign targeting, dates, and budgets,
 * implements the Cache-Aside pattern via Redis, selects winning ad via AdSelectionService,
 * records an impression asynchronously via Kafka, and produces the serving response.
 */
@Service
@RequiredArgsConstructor
public class AdServingService {

    private static final Logger log = LoggerFactory.getLogger(AdServingService.class);

    private final AdvertisementRepository advertisementRepository;
    private final AdSelectionService adSelectionService;
    private final ImpressionService impressionService;
    private final AdCacheService adCacheService;

    @Autowired(required = false)
    private AdEventProducer adEventProducer;

    /**
     * Serves an eligible advertisement matching the client's targeting criteria.
     * Implements Cache-Aside: checks Redis first; on miss/failure, queries MySQL,
     * caches eligible candidates with TTL, and selects the winner.
     *
     * @param request Targeting context (country, device, category)
     * @return AdResponseDto containing ad creative and destination details
     * @throws NoEligibleAdException if no advertisement matches the eligibility rules
     */
    @Transactional(readOnly = true)
    public AdResponseDto serveAd(AdRequestDto request) {
        String country = request.getCountry().trim().toUpperCase();
        String device = request.getDevice().trim().toUpperCase();
        String category = request.getCategory().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        // 1. Check Redis cache first (Cache-Aside Pattern)
        Optional<List<CachedAdDto>> cachedCandidates = adCacheService.getEligibleAds(country, device, category);

        List<Advertisement> eligibleAds;

        if (cachedCandidates.isPresent()) {
            // Redis HIT: reconstruct transient candidate entities from cached snapshot
            eligibleAds = cachedCandidates.get().stream()
                    .map(CachedAdDto::toEntity)
                    .collect(Collectors.toList());
            log.debug("Redis HIT: retrieved {} cached eligible candidates for {}:{}:{}",
                    eligibleAds.size(), country, device, category);
        } else {
            // Redis MISS or Redis outage: fetch active ads from MySQL and filter
            List<Advertisement> activeAds = advertisementRepository.findAllWithCampaignByStatus(AdStatus.ACTIVE);

            eligibleAds = activeAds.stream()
                    .filter(ad -> isEligible(ad, request, now))
                    .collect(Collectors.toList());

            log.debug("Redis MISS: evaluated {} eligible ads from MySQL for {}:{}:{}",
                    eligibleAds.size(), country, device, category);

            // Store filtered eligible candidates in Redis with TTL
            List<CachedAdDto> candidateDtos = eligibleAds.stream()
                    .map(CachedAdDto::fromEntity)
                    .collect(Collectors.toList());
            adCacheService.cacheEligibleAds(country, device, category, candidateDtos);
        }

        // 2. Select winning advertisement via pluggable strategy
        Advertisement selectedAd = adSelectionService.selectAd(eligibleAds)
                .orElseThrow(() -> new NoEligibleAdException(
                        request.getCountry(), request.getDevice(), request.getCategory()));

        // 3. Record impression for the served advertisement asynchronously via Kafka
        if (adEventProducer != null) {
            adEventProducer.publishImpression(AdImpressionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .adId(selectedAd.getId())
                    .campaignId(selectedAd.getCampaign() != null ? selectedAd.getCampaign().getId() : null)
                    .country(country)
                    .device(device)
                    .category(category)
                    .timestamp(now)
                    .build());
        } else if (impressionService != null) {
            impressionService.recordImpression(selectedAd.getId());
        }

        // 4. Build and return AdResponseDto
        return AdResponseDto.builder()
                .adId(selectedAd.getId())
                .campaignId(selectedAd.getCampaign().getId())
                .title(selectedAd.getTitle())
                .imageUrl(selectedAd.getImageUrl())
                .targetUrl(selectedAd.getTargetUrl())
                .build();
    }

    /**
     * Checks all 7 eligibility criteria for an advertisement:
     * 1. Ad status is ACTIVE
     * 2. Campaign status is ACTIVE
     * 3. Current time is between campaign start and end dates
     * 4. Campaign has remaining budget (> 0)
     * 5. Country matches (case-insensitive)
     * 6. Device matches (case-insensitive)
     * 7. Category matches (case-insensitive)
     */
    public boolean isEligible(Advertisement ad, AdRequestDto request, LocalDateTime currentTime) {
        // Rule 1: Ad status active
        if (ad.getStatus() != AdStatus.ACTIVE) {
            return false;
        }

        Campaign campaign = ad.getCampaign();
        if (campaign == null) {
            return false;
        }

        // Rule 2: Campaign status active
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            return false;
        }

        // Rule 3: Campaign active date range
        if (campaign.getStartDate() == null || campaign.getEndDate() == null) {
            return false;
        }
        if (currentTime.isBefore(campaign.getStartDate()) || currentTime.isAfter(campaign.getEndDate())) {
            return false;
        }

        // Rule 4: Campaign remaining budget
        if (campaign.getBudget() == null || campaign.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Rule 5: Country targeting match
        if (campaign.getTargetCountry() == null ||
                !campaign.getTargetCountry().equalsIgnoreCase(request.getCountry().trim())) {
            return false;
        }

        // Rule 6: Device targeting match
        if (campaign.getTargetDevice() == null ||
                !campaign.getTargetDevice().equalsIgnoreCase(request.getDevice().trim())) {
            return false;
        }

        // Rule 7: Category targeting match
        if (campaign.getTargetCategory() == null ||
                !campaign.getTargetCategory().equalsIgnoreCase(request.getCategory().trim())) {
            return false;
        }

        return true;
    }
}
