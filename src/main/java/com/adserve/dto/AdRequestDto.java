package com.adserve.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming ad request parameter DTO capturing client targeting context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdRequestDto {

    @NotBlank(message = "Country parameter is required (e.g. IN, US)")
    private String country;

    @NotBlank(message = "Device parameter is required (e.g. ANDROID, IOS, DESKTOP)")
    private String device;

    @NotBlank(message = "Category parameter is required (e.g. GAMING, FINANCE)")
    private String category;
}
