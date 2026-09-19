package com.adserve.service;

import com.adserve.entity.Advertisement;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic ad selection strategy.
 * Sorts eligible ads by campaign remaining budget in descending order (highest budget first),
 * then by ad ID in ascending order for stable deterministic tie-breaking.
 */
@Service
public class DeterministicAdSelectionService implements AdSelectionService {

    @Override
    public Optional<Advertisement> selectAd(List<Advertisement> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .sorted(Comparator
                        .comparing((Advertisement ad) -> ad.getCampaign().getBudget(), Comparator.reverseOrder())
                        .thenComparing(Advertisement::getId))
                .findFirst();
    }
}
