package com.adserve.service;

import com.adserve.dto.AdAnalyticsDto;
import com.adserve.dto.CampaignAnalyticsDto;
import com.adserve.dto.DashboardOverviewDto;
import com.adserve.entity.Advertisement;
import com.adserve.entity.Campaign;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service computing advertisement and campaign performance analytics,
 * Click-Through Rate (CTR), and dashboard KPI overviews.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ImpressionService impressionService;
    private final ClickService clickService;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertiserRepository advertiserRepository;
    private final CampaignRepository campaignRepository;
    private final AdImpressionRepository adImpressionRepository;
    private final AdClickRepository adClickRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.adserve.security.SecurityUtils securityUtils;

    /**
     * Calculates analytics metrics for a single advertisement including impressions, clicks, and CTR percentage.
     * Enforces ownership check: advertisers can only view analytics for their own ads.
     *
     * @param adId Advertisement ID
     * @return AdAnalyticsDto containing metrics
     * @throws ResourceNotFoundException if advertisement does not exist
     */
    public AdAnalyticsDto getAnalytics(Long adId) {
        if (!advertisementRepository.existsById(adId)) {
            throw new ResourceNotFoundException("Advertisement", "id", adId);
        }

        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isAdOwner(adId)) {
                throw new com.adserve.exception.ForbiddenException("Advertisement Analytics", adId);
            }
        }

        long impressions = impressionService.getImpressionCount(adId);
        long clicks = clickService.getClickCount(adId);

        double ctr = calculateCtr(clicks, impressions);

        return AdAnalyticsDto.builder()
                .adId(adId)
                .impressions(impressions)
                .clicks(clicks)
                .ctr(ctr)
                .build();
    }

    /**
     * Calculates aggregated dashboard KPI statistics across the entire platform.
     *
     * @return DashboardOverviewDto with aggregate totals and average CTR
     */
    public DashboardOverviewDto getDashboardOverview() {
        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvId = securityUtils.getCurrentAdvertiserId();
            if (callerAdvId != null) {
                List<Campaign> myCampaigns = campaignRepository.findByAdvertiserId(callerAdvId);
                List<Advertisement> myAds = advertisementRepository.findByAdvertiserId(callerAdvId);
                long totalImpressions = 0;
                long totalClicks = 0;

                for (Advertisement ad : myAds) {
                    totalImpressions += impressionService.getImpressionCount(ad.getId());
                    totalClicks += clickService.getClickCount(ad.getId());
                }

                double avgCtr = calculateCtr(totalClicks, totalImpressions);

                return DashboardOverviewDto.builder()
                        .totalAdvertisers(1L)
                        .totalCampaigns((long) myCampaigns.size())
                        .totalAdvertisements((long) myAds.size())
                        .totalImpressions(totalImpressions)
                        .totalClicks(totalClicks)
                        .averageCtr(avgCtr)
                        .build();
            }
        }

        long totalAdvertisers = advertiserRepository.count();
        long totalCampaigns = campaignRepository.count();
        long totalAdvertisements = advertisementRepository.count();
        long totalImpressions = adImpressionRepository.count();
        long totalClicks = adClickRepository.count();
        double averageCtr = calculateCtr(totalClicks, totalImpressions);

        return DashboardOverviewDto.builder()
                .totalAdvertisers(totalAdvertisers)
                .totalCampaigns(totalCampaigns)
                .totalAdvertisements(totalAdvertisements)
                .totalImpressions(totalImpressions)
                .totalClicks(totalClicks)
                .averageCtr(averageCtr)
                .build();
    }

    /**
     * Calculates campaign-level performance breakdown (impressions, clicks, CTR).
     * Filtered to own campaigns if caller is an ADVERTISER.
     *
     * @return List of CampaignAnalyticsDto
     */
    public List<CampaignAnalyticsDto> getCampaignAnalytics() {
        List<Campaign> campaigns;
        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvId = securityUtils.getCurrentAdvertiserId();
            campaigns = callerAdvId != null ? campaignRepository.findByAdvertiserId(callerAdvId) : List.of();
        } else {
            campaigns = campaignRepository.findAll();
        }

        List<CampaignAnalyticsDto> result = new ArrayList<>();

        for (Campaign campaign : campaigns) {
            List<Advertisement> ads = advertisementRepository.findByCampaignId(campaign.getId());
            long campaignImpressions = 0;
            long campaignClicks = 0;

            for (Advertisement ad : ads) {
                campaignImpressions += impressionService.getImpressionCount(ad.getId());
                campaignClicks += clickService.getClickCount(ad.getId());
            }

            double campaignCtr = calculateCtr(campaignClicks, campaignImpressions);

            result.add(CampaignAnalyticsDto.builder()
                    .campaignId(campaign.getId())
                    .campaignName(campaign.getName())
                    .advertiserName(campaign.getAdvertiser() != null ? campaign.getAdvertiser().getName() : "Unknown")
                    .status(campaign.getStatus() != null ? campaign.getStatus().name() : "UNKNOWN")
                    .budget(campaign.getBudget())
                    .impressions(campaignImpressions)
                    .clicks(campaignClicks)
                    .ctr(campaignCtr)
                    .build());
        }

        return result;
    }

    /**
     * Calculates Click-Through Rate (CTR) percentage: (clicks / impressions) * 100.
     * Handles zero-impression case safely by returning 0.0.
     *
     * @param clicks Number of clicks
     * @param impressions Number of impressions
     * @return CTR rounded to 2 decimal places
     */
    public double calculateCtr(long clicks, long impressions) {
        if (impressions <= 0) {
            return 0.0;
        }

        double rawCtr = ((double) clicks / (double) impressions) * 100.0;
        return BigDecimal.valueOf(rawCtr)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
