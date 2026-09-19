package com.adserve.service;

import com.adserve.dto.CampaignRequestDto;
import com.adserve.dto.CampaignResponseDto;
import com.adserve.entity.Advertiser;
import com.adserve.entity.Campaign;
import com.adserve.exception.BadRequestException;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing campaign lifecycles, targeting, and validations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AdvertiserService advertiserService;
    private final AdCacheService adCacheService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.adserve.security.SecurityUtils securityUtils;

    /**
     * Creates a new campaign with business rule validations.
     * Enforces that an advertiser can only create campaigns for their own account.
     * Invalidates any cached eligible ads for the new targeting parameters.
     *
     * @param requestDto Campaign creation payload
     * @return Created CampaignResponseDto
     */
    @Transactional
    public CampaignResponseDto createCampaign(CampaignRequestDto requestDto) {
        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvertiserId = securityUtils.getCurrentAdvertiserId();
            if (callerAdvertiserId != null && !callerAdvertiserId.equals(requestDto.getAdvertiserId())) {
                throw new com.adserve.exception.ForbiddenException("Advertisers can only create campaigns for their own account");
            }
        }

        validateCampaignDatesAndBudget(requestDto);

        Advertiser advertiser = advertiserService.getAdvertiserEntity(requestDto.getAdvertiserId());

        Campaign campaign = Campaign.builder()
                .advertiser(advertiser)
                .name(requestDto.getName().trim())
                .budget(requestDto.getBudget())
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .status(requestDto.getStatus())
                .targetCountry(requestDto.getTargetCountry().trim().toUpperCase())
                .targetDevice(requestDto.getTargetDevice().trim().toUpperCase())
                .targetCategory(requestDto.getTargetCategory().trim().toUpperCase())
                .build();

        Campaign saved = campaignRepository.save(campaign);
        adCacheService.evictCampaignCache(saved);
        return mapToResponseDto(saved);
    }

    /**
     * Retrieves campaigns. If the caller is an ADVERTISER, only returns their own campaigns.
     *
     * @return List of CampaignResponseDto
     */
    public List<CampaignResponseDto> getAllCampaigns() {
        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvertiserId = securityUtils.getCurrentAdvertiserId();
            if (callerAdvertiserId != null) {
                return campaignRepository.findByAdvertiserId(callerAdvertiserId)
                        .stream()
                        .map(this::mapToResponseDto)
                        .collect(Collectors.toList());
            }
        }

        return campaignRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Finds a campaign by ID.
     * Enforces ownership check: advertisers can only access their own campaigns.
     *
     * @param id Campaign ID
     * @return CampaignResponseDto
     */
    public CampaignResponseDto getCampaignById(Long id) {
        verifyCampaignOwnership(id);
        Campaign campaign = getCampaignEntity(id);
        return mapToResponseDto(campaign);
    }

    /**
     * Updates an existing campaign.
     * Invalidates cache for both old targeting parameters and new targeting parameters.
     *
     * @param id Campaign ID
     * @param requestDto Update payload
     * @return Updated CampaignResponseDto
     */
    @Transactional
    public CampaignResponseDto updateCampaign(Long id, CampaignRequestDto requestDto) {
        verifyCampaignOwnership(id);

        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvId = securityUtils.getCurrentAdvertiserId();
            if (callerAdvId != null && !callerAdvId.equals(requestDto.getAdvertiserId())) {
                throw new com.adserve.exception.ForbiddenException("Advertisers cannot transfer campaigns to another account");
            }
        }

        Campaign existing = getCampaignEntity(id);
        validateCampaignDatesAndBudget(requestDto);

        String oldCountry = existing.getTargetCountry();
        String oldDevice = existing.getTargetDevice();
        String oldCategory = existing.getTargetCategory();

        Advertiser advertiser = advertiserService.getAdvertiserEntity(requestDto.getAdvertiserId());

        existing.setAdvertiser(advertiser);
        existing.setName(requestDto.getName().trim());
        existing.setBudget(requestDto.getBudget());
        existing.setStartDate(requestDto.getStartDate());
        existing.setEndDate(requestDto.getEndDate());
        existing.setStatus(requestDto.getStatus());
        existing.setTargetCountry(requestDto.getTargetCountry().trim().toUpperCase());
        existing.setTargetDevice(requestDto.getTargetDevice().trim().toUpperCase());
        existing.setTargetCategory(requestDto.getTargetCategory().trim().toUpperCase());

        Campaign updated = campaignRepository.save(existing);

        // Invalidate old targeting and new targeting caches
        adCacheService.evictByTargeting(oldCountry, oldDevice, oldCategory);
        adCacheService.evictCampaignCache(updated);

        return mapToResponseDto(updated);
    }

    /**
     * Deletes a campaign by ID.
     * Invalidates cached eligible ads for the deleted campaign's targeting.
     *
     * @param id Campaign ID
     */
    @Transactional
    public void deleteCampaign(Long id) {
        verifyCampaignOwnership(id);
        Campaign campaign = getCampaignEntity(id);
        adCacheService.evictCampaignCache(campaign);
        campaignRepository.delete(campaign);
    }

    private void verifyCampaignOwnership(Long campaignId) {
        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isCampaignOwner(campaignId)) {
                throw new com.adserve.exception.ForbiddenException("Campaign", campaignId);
            }
        }
    }

    /**
     * Internal lookup for Campaign entity reference.
     *
     * @param id Campaign ID
     * @return Campaign entity
     */
    public Campaign getCampaignEntity(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", id));
    }

    /**
     * Validates that budget is strictly positive and end date is not before start date.
     */
    private void validateCampaignDatesAndBudget(CampaignRequestDto dto) {
        if (dto.getBudget() == null || dto.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Campaign budget must be greater than zero");
        }
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BadRequestException("Campaign end date cannot be before start date");
            }
        }
    }

    private CampaignResponseDto mapToResponseDto(Campaign campaign) {
        return CampaignResponseDto.builder()
                .id(campaign.getId())
                .advertiserId(campaign.getAdvertiser().getId())
                .advertiserName(campaign.getAdvertiser().getName())
                .name(campaign.getName())
                .budget(campaign.getBudget())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .targetCountry(campaign.getTargetCountry())
                .targetDevice(campaign.getTargetDevice())
                .targetCategory(campaign.getTargetCategory())
                .createdAt(campaign.getCreatedAt())
                .adCount(campaign.getAdvertisements() != null ? campaign.getAdvertisements().size() : 0)
                .build();
    }
}
