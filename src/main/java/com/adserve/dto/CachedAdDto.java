package com.adserve.dto;

import com.adserve.entity.AdStatus;
import com.adserve.entity.Advertisement;
import com.adserve.entity.Campaign;
import com.adserve.entity.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Clean, JSON-serializable DTO representing an eligible candidate advertisement stored in Redis.
 * Decoupled from JPA Hibernate entities to avoid lazy-loading and circular serialization issues.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedAdDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long adId;
    private Long campaignId;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private BigDecimal budget;
    private String targetCountry;
    private String targetDevice;
    private String targetCategory;
    private AdStatus status;
    private CampaignStatus campaignStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    /**
     * Creates a CachedAdDto snapshot from a persistent Advertisement entity.
     *
     * @param ad Source advertisement entity with campaign loaded
     * @return Serializable CachedAdDto
     */
    public static CachedAdDto fromEntity(Advertisement ad) {
        if (ad == null) {
            return null;
        }

        Campaign campaign = ad.getCampaign();
        return CachedAdDto.builder()
                .adId(ad.getId())
                .campaignId(campaign != null ? campaign.getId() : null)
                .title(ad.getTitle())
                .imageUrl(ad.getImageUrl())
                .targetUrl(ad.getTargetUrl())
                .budget(campaign != null ? campaign.getBudget() : BigDecimal.ZERO)
                .targetCountry(campaign != null ? campaign.getTargetCountry() : null)
                .targetDevice(campaign != null ? campaign.getTargetDevice() : null)
                .targetCategory(campaign != null ? campaign.getTargetCategory() : null)
                .status(ad.getStatus())
                .campaignStatus(campaign != null ? campaign.getStatus() : null)
                .startDate(campaign != null ? campaign.getStartDate() : null)
                .endDate(campaign != null ? campaign.getEndDate() : null)
                .build();
    }

    /**
     * Reconstructs a transient Advertisement entity suitable for AdSelectionService.
     *
     * @return Transient Advertisement entity populated with campaign and budget
     */
    public Advertisement toEntity() {
        Campaign campaign = Campaign.builder()
                .id(this.campaignId)
                .budget(this.budget != null ? this.budget : BigDecimal.ZERO)
                .status(this.campaignStatus)
                .targetCountry(this.targetCountry)
                .targetDevice(this.targetDevice)
                .targetCategory(this.targetCategory)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .build();

        return Advertisement.builder()
                .id(this.adId)
                .title(this.title)
                .imageUrl(this.imageUrl)
                .targetUrl(this.targetUrl)
                .status(this.status)
                .campaign(campaign)
                .build();
    }
}
