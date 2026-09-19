package com.adserve.service;

import com.adserve.entity.AdClick;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.AdClickRepository;
import com.adserve.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing advertisement click tracking and counts.
 */
@Service
@RequiredArgsConstructor
public class ClickService {

    private static final Logger log = LoggerFactory.getLogger(ClickService.class);

    private final AdClickRepository adClickRepository;
    private final AdvertisementRepository advertisementRepository;

    /**
     * Records a click event for a valid advertisement in MySQL.
     *
     * @param adId Advertisement ID
     * @return Saved AdClick entity
     * @throws ResourceNotFoundException if ad does not exist
     */
    @Transactional
    public AdClick recordClick(Long adId) {
        if (!advertisementRepository.existsById(adId)) {
            throw new ResourceNotFoundException("Advertisement", "id", adId);
        }

        AdClick click = AdClick.builder()
                .adId(adId)
                .build();

        AdClick saved = adClickRepository.save(click);
        log.debug("Recorded click for adId: {}, clickId: {}", adId, saved.getId());
        return saved;
    }

    /**
     * Retrieves total click count for a specific advertisement.
     *
     * @param adId Advertisement ID
     * @return Total click count
     */
    @Transactional(readOnly = true)
    public long getClickCount(Long adId) {
        return adClickRepository.countByAdId(adId);
    }
}
