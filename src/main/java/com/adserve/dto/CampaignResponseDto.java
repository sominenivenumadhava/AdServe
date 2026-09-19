package com.adserve.dto;

import com.adserve.entity.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a campaign with targeting and budget attributes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponseDto {

    private Long id;
    private Long advertiserId;
    private String advertiserName;
    private String name;
    private BigDecimal budget;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private CampaignStatus status;
    private String targetCountry;
    private String targetDevice;
    private String targetCategory;
    private LocalDateTime createdAt;
    private int adCount;
}
