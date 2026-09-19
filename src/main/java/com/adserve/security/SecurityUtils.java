package com.adserve.security;

import com.adserve.entity.Advertisement;
import com.adserve.entity.Campaign;
import com.adserve.entity.Role;
import com.adserve.repository.AdvertisementRepository;
import com.adserve.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper component for retrieving authentication details and enforcing multi-tenant ownership.
 */
@Component("securityUtils")
@RequiredArgsConstructor
public class SecurityUtils {

    private final CampaignRepository campaignRepository;
    private final AdvertisementRepository advertisementRepository;

    /**
     * Retrieves the UserPrincipal for the currently authenticated caller.
     */
    public Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) auth.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * Checks if caller has ADMIN role.
     */
    public boolean isAdmin() {
        return getCurrentUserPrincipal()
                .map(p -> p.getRole() == Role.ADMIN)
                .orElse(false);
    }

    /**
     * Checks if caller has ADVERTISER role.
     */
    public boolean isAdvertiser() {
        return getCurrentUserPrincipal()
                .map(p -> p.getRole() == Role.ADVERTISER)
                .orElse(false);
    }

    /**
     * Returns the linked advertiserId of the authenticated caller, if any.
     */
    public Long getCurrentAdvertiserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getAdvertiserId)
                .orElse(null);
    }

    /**
     * Verifies if the authenticated caller owns the specified campaign.
     * ADMIN always returns true.
     * ADVERTISER returns true only if campaign.advertiser.id == caller.advertiserId.
     */
    public boolean isCampaignOwner(Long campaignId) {
        if (isAdmin()) {
            return true;
        }
        Long callerAdvertiserId = getCurrentAdvertiserId();
        if (callerAdvertiserId == null) {
            return false;
        }

        return campaignRepository.findById(campaignId)
                .map(Campaign::getAdvertiser)
                .map(advertiser -> advertiser.getId().equals(callerAdvertiserId))
                .orElse(false);
    }

    /**
     * Verifies if the authenticated caller owns the advertisement creative.
     * ADMIN always returns true.
     * ADVERTISER returns true only if ad.campaign.advertiser.id == caller.advertiserId.
     */
    public boolean isAdOwner(Long adId) {
        if (isAdmin()) {
            return true;
        }
        Long callerAdvertiserId = getCurrentAdvertiserId();
        if (callerAdvertiserId == null) {
            return false;
        }

        return advertisementRepository.findById(adId)
                .map(Advertisement::getCampaign)
                .map(Campaign::getAdvertiser)
                .map(advertiser -> advertiser.getId().equals(callerAdvertiserId))
                .orElse(false);
    }

    /**
     * Verifies if the caller owns the specified advertiser account.
     */
    public boolean isAdvertiserOwner(Long advertiserId) {
        if (isAdmin()) {
            return true;
        }
        Long callerAdvertiserId = getCurrentAdvertiserId();
        return callerAdvertiserId != null && callerAdvertiserId.equals(advertiserId);
    }
}
