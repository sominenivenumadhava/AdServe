package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clean outgoing ad response DTO returned to the publisher or client application.
 * Hides internal database schema and exposes only serving payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponseDto {

    private Long adId;
    private Long campaignId;
    private String title;
    private String imageUrl;
    private String targetUrl;
}
