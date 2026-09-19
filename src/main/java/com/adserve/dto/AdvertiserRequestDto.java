package com.adserve.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating an advertiser.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertiserRequestDto {

    @NotBlank(message = "Advertiser name is required")
    @Size(max = 100, message = "Advertiser name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;
}
