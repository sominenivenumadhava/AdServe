package com.adserve.dto;

import com.adserve.entity.AdStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload representing an advertisement creative.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementResponseDto {

    private Long id;
    private Long campaignId;
    private String campaignName;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private AdStatus status;
    private LocalDateTime createdAt;
}
