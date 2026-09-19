package com.adserve.controller;

import com.adserve.dto.ApiResponse;
import com.adserve.dto.CampaignRequestDto;
import com.adserve.dto.CampaignResponseDto;
import com.adserve.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing advertising campaigns.
 */
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    /**
     * Creates a new campaign.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponseDto>> createCampaign(
            @Valid @RequestBody CampaignRequestDto requestDto) {
        CampaignResponseDto created = campaignService.createCampaign(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Campaign created successfully"));
    }

    /**
     * Lists all campaigns.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponseDto>>> getAllCampaigns() {
        List<CampaignResponseDto> campaigns = campaignService.getAllCampaigns();
        return ResponseEntity.ok(ApiResponse.success(campaigns, "Campaigns retrieved successfully"));
    }

    /**
     * Retrieves a campaign by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponseDto>> getCampaignById(@PathVariable Long id) {
        CampaignResponseDto campaign = campaignService.getCampaignById(id);
        return ResponseEntity.ok(ApiResponse.success(campaign, "Campaign found"));
    }

    /**
     * Updates an existing campaign.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponseDto>> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignRequestDto requestDto) {
        CampaignResponseDto updated = campaignService.updateCampaign(id, requestDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Campaign updated successfully"));
    }

    /**
     * Deletes a campaign by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Campaign deleted successfully"));
    }
}
