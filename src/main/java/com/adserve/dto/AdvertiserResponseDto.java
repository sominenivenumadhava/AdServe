package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload representing an advertiser.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertiserResponseDto {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private int campaignCount;
}
