package com.adserve.service;

import com.adserve.dto.AdvertiserRequestDto;
import com.adserve.dto.AdvertiserResponseDto;
import com.adserve.entity.Advertiser;
import com.adserve.exception.DuplicateResourceException;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.AdvertiserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing advertiser account registration, lookup, and validations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvertiserService {

    private final AdvertiserRepository advertiserRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.adserve.security.SecurityUtils securityUtils;

    /**
     * Registers a new advertiser after verifying email uniqueness.
     *
     * @param requestDto Advertiser creation payload
     * @return Created AdvertiserResponseDto
     * @throws DuplicateResourceException if email is already registered
     */
    @Transactional
    public AdvertiserResponseDto createAdvertiser(AdvertiserRequestDto requestDto) {
        if (securityUtils != null && !securityUtils.isAdmin() && securityUtils.isAdvertiser()) {
            throw new com.adserve.exception.ForbiddenException("Only administrators can manually create arbitrary advertiser accounts");
        }

        if (advertiserRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("Advertiser", "email", requestDto.getEmail());
        }

        Advertiser advertiser = Advertiser.builder()
                .name(requestDto.getName().trim())
                .email(requestDto.getEmail().trim().toLowerCase())
                .build();

        Advertiser saved = advertiserRepository.save(advertiser);
        return mapToResponseDto(saved);
    }

    /**
     * Retrieves all registered advertisers.
     * If caller is an ADVERTISER, only returns their own profile.
     *
     * @return List of AdvertiserResponseDto
     */
    public List<AdvertiserResponseDto> getAllAdvertisers() {
        if (securityUtils != null && securityUtils.isAdvertiser()) {
            Long callerAdvertiserId = securityUtils.getCurrentAdvertiserId();
            if (callerAdvertiserId != null) {
                return advertiserRepository.findById(callerAdvertiserId)
                        .map(this::mapToResponseDto)
                        .map(List::of)
                        .orElse(List.of());
            }
        }

        return advertiserRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Finds an advertiser by ID.
     * Enforces ownership check: advertisers can only access their own profile.
     *
     * @param id Advertiser ID
     * @return AdvertiserResponseDto
     * @throws ResourceNotFoundException if advertiser does not exist
     */
    public AdvertiserResponseDto getAdvertiserById(Long id) {
        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isAdvertiserOwner(id)) {
                throw new com.adserve.exception.ForbiddenException("Advertiser", id);
            }
        }
        Advertiser advertiser = getAdvertiserEntity(id);
        return mapToResponseDto(advertiser);
    }

    /**
     * Internal lookup for Advertiser entity reference.
     *
     * @param id Advertiser ID
     * @return Advertiser entity
     */
    public Advertiser getAdvertiserEntity(Long id) {
        return advertiserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertiser", "id", id));
    }

    /**
     * Updates an existing advertiser's name and email.
     *
     * @param id Advertiser ID
     * @param requestDto Update payload
     * @return Updated AdvertiserResponseDto
     * @throws DuplicateResourceException if updated email already belongs to another advertiser
     */
    @Transactional
    public AdvertiserResponseDto updateAdvertiser(Long id, AdvertiserRequestDto requestDto) {
        if (securityUtils != null && !securityUtils.isAdmin()) {
            if (securityUtils.isAdvertiser() && !securityUtils.isAdvertiserOwner(id)) {
                throw new com.adserve.exception.ForbiddenException("Advertiser", id);
            }
        }

        Advertiser existing = getAdvertiserEntity(id);
        String newEmail = requestDto.getEmail().trim().toLowerCase();
        if (!existing.getEmail().equalsIgnoreCase(newEmail) && advertiserRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Advertiser", "email", newEmail);
        }

        existing.setName(requestDto.getName().trim());
        existing.setEmail(newEmail);

        Advertiser updated = advertiserRepository.save(existing);
        return mapToResponseDto(updated);
    }

    /**
     * Deletes an advertiser by ID.
     * Only platform administrators can delete advertiser accounts.
     *
     * @param id Advertiser ID
     */
    @Transactional
    public void deleteAdvertiser(Long id) {
        if (securityUtils != null && !securityUtils.isAdmin() && securityUtils.isAdvertiser()) {
            throw new com.adserve.exception.ForbiddenException("Only administrators can delete advertiser accounts");
        }
        Advertiser advertiser = getAdvertiserEntity(id);
        advertiserRepository.delete(advertiser);
    }

    private AdvertiserResponseDto mapToResponseDto(Advertiser advertiser) {
        return AdvertiserResponseDto.builder()
                .id(advertiser.getId())
                .name(advertiser.getName())
                .email(advertiser.getEmail())
                .createdAt(advertiser.getCreatedAt())
                .campaignCount(advertiser.getCampaigns() != null ? advertiser.getCampaigns().size() : 0)
                .build();
    }
}
