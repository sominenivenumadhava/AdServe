package com.adserve.service;

import com.adserve.entity.AdImpression;
import com.adserve.repository.AdImpressionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing advertisement impression recording and counts.
 */
@Service
@RequiredArgsConstructor
public class ImpressionService {

    private static final Logger log = LoggerFactory.getLogger(ImpressionService.class);

    private final AdImpressionRepository adImpressionRepository;

    /**
     * Records an impression event in MySQL.
     *
     * @param adId Advertisement ID
     * @return Saved AdImpression entity
     */
    @Transactional
    public AdImpression recordImpression(Long adId) {
        AdImpression impression = AdImpression.builder()
                .adId(adId)
                .build();

        AdImpression saved = adImpressionRepository.save(impression);
        log.debug("Recorded impression for adId: {}, impressionId: {}", adId, saved.getId());
        return saved;
    }

    /**
     * Retrieves total impression count for a specific advertisement.
     *
     * @param adId Advertisement ID
     * @return Total impression count
     */
    @Transactional(readOnly = true)
    public long getImpressionCount(Long adId) {
        return adImpressionRepository.countByAdId(adId);
    }
}
