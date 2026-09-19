package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level dashboard summary metrics DTO for the React admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {

    private long totalAdvertisers;
    private long totalCampaigns;
    private long totalAdvertisements;
    private long totalImpressions;
    private long totalClicks;
    private double averageCtr;
}
