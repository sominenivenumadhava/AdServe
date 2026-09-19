package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics reporting DTO for an advertisement including impressions, clicks, and Click-Through Rate (CTR).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAnalyticsDto {

    private Long adId;
    private long impressions;
    private long clicks;
    private double ctr;
}
