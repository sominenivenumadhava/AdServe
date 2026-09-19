package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing authenticated user details (never includes password or passwordHash).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Long advertiserId;
}
