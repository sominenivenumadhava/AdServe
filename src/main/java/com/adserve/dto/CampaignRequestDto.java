package com.adserve.dto;

import com.adserve.entity.CampaignStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for creating or updating a campaign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequestDto {

    @NotNull(message = "Advertiser ID is required")
    private Long advertiserId;

    @NotBlank(message = "Campaign name is required")
    @Size(max = 150, message = "Campaign name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Campaign budget must be greater than zero")
    private BigDecimal budget;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Campaign status is required (ACTIVE, PAUSED, COMPLETED)")
    private CampaignStatus status;

    @NotBlank(message = "Target country is required (e.g. IN, US, GB)")
    @Size(max = 10, message = "Target country code must not exceed 10 characters")
    private String targetCountry;

    @NotBlank(message = "Target device is required (e.g. ANDROID, IOS, DESKTOP)")
    @Size(max = 30, message = "Target device must not exceed 30 characters")
    private String targetDevice;

    @NotBlank(message = "Target category is required (e.g. GAMING, FINANCE, ECOMMERCE)")
    @Size(max = 50, message = "Target category must not exceed 50 characters")
    private String targetCategory;
}
