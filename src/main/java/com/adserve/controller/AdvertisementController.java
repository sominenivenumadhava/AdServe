package com.adserve.controller;

import com.adserve.dto.ApiResponse;
import com.adserve.dto.AdvertisementRequestDto;
import com.adserve.dto.AdvertisementResponseDto;
import com.adserve.service.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing advertisement creative assets.
 */
@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    /**
     * Creates a new advertisement under an existing campaign.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AdvertisementResponseDto>> createAdvertisement(
            @Valid @RequestBody AdvertisementRequestDto requestDto) {
        AdvertisementResponseDto created = advertisementService.createAdvertisement(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Advertisement created successfully"));
    }

    /**
     * Lists all advertisements.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdvertisementResponseDto>>> getAllAdvertisements() {
        List<AdvertisementResponseDto> ads = advertisementService.getAllAdvertisements();
        return ResponseEntity.ok(ApiResponse.success(ads, "Advertisements retrieved successfully"));
    }

    /**
     * Retrieves an advertisement by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdvertisementResponseDto>> getAdvertisementById(@PathVariable Long id) {
        AdvertisementResponseDto ad = advertisementService.getAdvertisementById(id);
        return ResponseEntity.ok(ApiResponse.success(ad, "Advertisement found"));
    }

    /**
     * Updates an existing advertisement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdvertisementResponseDto>> updateAdvertisement(
            @PathVariable Long id,
            @Valid @RequestBody AdvertisementRequestDto requestDto) {
        AdvertisementResponseDto updated = advertisementService.updateAdvertisement(id, requestDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Advertisement updated successfully"));
    }

    /**
     * Deletes an advertisement by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdvertisement(@PathVariable Long id) {
        advertisementService.deleteAdvertisement(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Advertisement deleted successfully"));
    }
}
