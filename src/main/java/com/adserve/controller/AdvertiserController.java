package com.adserve.controller;

import com.adserve.dto.ApiResponse;
import com.adserve.dto.AdvertiserRequestDto;
import com.adserve.dto.AdvertiserResponseDto;
import com.adserve.service.AdvertiserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing advertiser accounts.
 */
@RestController
@RequestMapping("/api/advertisers")
@RequiredArgsConstructor
public class AdvertiserController {

    private final AdvertiserService advertiserService;

    /**
     * Registers a new advertiser.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AdvertiserResponseDto>> createAdvertiser(
            @Valid @RequestBody AdvertiserRequestDto requestDto) {
        AdvertiserResponseDto created = advertiserService.createAdvertiser(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Advertiser registered successfully"));
    }

    /**
     * Lists all registered advertisers.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdvertiserResponseDto>>> getAllAdvertisers() {
        List<AdvertiserResponseDto> advertisers = advertiserService.getAllAdvertisers();
        return ResponseEntity.ok(ApiResponse.success(advertisers, "Advertisers retrieved successfully"));
    }

    /**
     * Retrieves an advertiser by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdvertiserResponseDto>> getAdvertiserById(@PathVariable Long id) {
        AdvertiserResponseDto advertiser = advertiserService.getAdvertiserById(id);
        return ResponseEntity.ok(ApiResponse.success(advertiser, "Advertiser found"));
    }

    /**
     * Updates an existing advertiser.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdvertiserResponseDto>> updateAdvertiser(
            @PathVariable Long id,
            @Valid @RequestBody AdvertiserRequestDto requestDto) {
        AdvertiserResponseDto updated = advertiserService.updateAdvertiser(id, requestDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Advertiser updated successfully"));
    }

    /**
     * Deletes an advertiser by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdvertiser(@PathVariable Long id) {
        advertiserService.deleteAdvertiser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Advertiser deleted successfully"));
    }
}
