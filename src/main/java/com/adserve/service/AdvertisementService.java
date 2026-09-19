package com.adserve.service;

import com.adserve.dto.AdvertisementRequestDto;
import com.adserve.dto.AdvertisementResponseDto;
import com.adserve.entity.Advertisement;
import com.adserve.entity.Campaign;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing advertisement creative lifecycles and campaign associations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final CampaignService campaignService;
    private final AdCacheService adCacheService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.adserve.security.SecurityUtils securityUtils;

    /**
     * Creates a new advertisement associated with an existing campaign.
     * Invalidates cached eligible ads for the associated campaign.
     *
     * @param requestDto Advertisement creation payload
     * @return Created AdvertisementResponseDto
     */
    @Transactional
    public AdvertisementResponseDto createAdvertisement(AdvertisementRequestDto requestDto) {
        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isCampaignOwner(requestDto.getCampaignId())) {
                throw new com.adserve.exception.ForbiddenException("Cannot create advertisements for another advertiser's campaign");
            }
        }

        Campaign campaign = campaignService.getCampaignEntity(requestDto.getCampaignId());

        Advertisement advertisement = Advertisement.builder()
                .campaign(campaign)
                .title(requestDto.getTitle().trim())
                .imageUrl(requestDto.getImageUrl().trim())
                .targetUrl(requestDto.getTargetUrl().trim())
                .status(requestDto.getStatus())
                .build();

        Advertisement saved = advertisementRepository.save(advertisement);
        adCacheService.evictCampaignCache(campaign);
        return mapToResponseDto(saved);
    }

    /**
     * Retrieves all advertisements.
     *
     * @return List of AdvertisementResponseDto
     */
    public List<AdvertisementResponseDto> getAllAdvertisements() {
        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvertiserId = securityUtils.getCurrentAdvertiserId();
            if (callerAdvertiserId != null) {
                return advertisementRepository.findByAdvertiserId(callerAdvertiserId)
                        .stream()
                        .map(this::mapToResponseDto)
                        .collect(Collectors.toList());
            }
        }

        return advertisementRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Finds an advertisement creative by ID.
     * Enforces ownership check: advertisers can only access their own ads.
     *
     * @param id Advertisement ID
     * @return AdvertisementResponseDto
     */
    public AdvertisementResponseDto getAdvertisementById(Long id) {
        verifyAdOwnership(id);
        Advertisement advertisement = getAdvertisementEntity(id);
        return mapToResponseDto(advertisement);
    }

    /**
     * Updates an existing advertisement.
     * Invalidates cache for both the previous campaign (if altered) and the current campaign.
     *
     * @param id Advertisement ID
     * @param requestDto Update payload
     * @return Updated AdvertisementResponseDto
     */
    @Transactional
    public AdvertisementResponseDto updateAdvertisement(Long id, AdvertisementRequestDto requestDto) {
        verifyAdOwnership(id);
        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isCampaignOwner(requestDto.getCampaignId())) {
                throw new com.adserve.exception.ForbiddenException("Cannot assign advertisement to a campaign owned by another advertiser");
            }
        }

        Advertisement existing = getAdvertisementEntity(id);
        Campaign oldCampaign = existing.getCampaign();
        Campaign campaign = campaignService.getCampaignEntity(requestDto.getCampaignId());

        existing.setCampaign(campaign);
        existing.setTitle(requestDto.getTitle().trim());
        existing.setImageUrl(requestDto.getImageUrl().trim());
        existing.setTargetUrl(requestDto.getTargetUrl().trim());
        existing.setStatus(requestDto.getStatus());

        Advertisement updated = advertisementRepository.save(existing);

        // Invalidate old campaign targeting if different, and new campaign targeting
        if (oldCampaign != null && !oldCampaign.getId().equals(campaign.getId())) {
            adCacheService.evictCampaignCache(oldCampaign);
        }
        adCacheService.evictCampaignCache(campaign);

        return mapToResponseDto(updated);
    }

    /**
     * Deletes an advertisement creative by ID.
     * Invalidates cached eligible ads for the associated campaign.
     *
     * @param id Advertisement ID
     */
    @Transactional
    public void deleteAdvertisement(Long id) {
        verifyAdOwnership(id);
        Advertisement advertisement = getAdvertisementEntity(id);
        adCacheService.evictCampaignCache(advertisement.getCampaign());
        advertisementRepository.delete(advertisement);
    }

    private void verifyAdOwnership(Long adId) {
        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isAdOwner(adId)) {
                throw new com.adserve.exception.ForbiddenException("Advertisement", adId);
            }
        }
    }

    /**
     * Internal lookup for Advertisement entity reference.
     *
     * @param id Advertisement ID
     * @return Advertisement entity
     */
    public Advertisement getAdvertisementEntity(Long id) {
        return advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement", "id", id));
    }

    private AdvertisementResponseDto mapToResponseDto(Advertisement advertisement) {
        return AdvertisementResponseDto.builder()
                .id(advertisement.getId())
                .campaignId(advertisement.getCampaign().getId())
                .campaignName(advertisement.getCampaign().getName())
                .title(advertisement.getTitle())
                .imageUrl(advertisement.getImageUrl())
                .targetUrl(advertisement.getTargetUrl())
                .status(advertisement.getStatus())
                .createdAt(advertisement.getCreatedAt())
                .build();
    }
}
