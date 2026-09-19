package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Campaign-level performance analytics DTO used for Recharts visualization and campaign reporting tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAnalyticsDto {

    private Long campaignId;
    private String campaignName;
    private String advertiserName;
    private String status;
    private BigDecimal budget;
    private long impressions;
    private long clicks;
    private double ctr;
}
