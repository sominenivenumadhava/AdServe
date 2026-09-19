package com.adserve.dto;

import com.adserve.entity.AdStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating an advertisement creative.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementRequestDto {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotBlank(message = "Ad title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @NotBlank(message = "Target click URL is required")
    @Size(max = 500, message = "Target URL must not exceed 500 characters")
    private String targetUrl;

    @NotNull(message = "Ad status is required (ACTIVE, INACTIVE)")
    private AdStatus status;
}
