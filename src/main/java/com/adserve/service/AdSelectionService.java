package com.adserve.service;

import com.adserve.entity.Advertisement;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting an advertisement among eligible candidates.
 * Designed to allow pluggable algorithms (e.g. deterministic budget priority, eCPM auction, pacing, weighted random).
 */
public interface AdSelectionService {

    /**
     * Selects a single winning advertisement from the list of eligible candidates.
     *
     * @param candidates Filtered list of eligible advertisements
     * @return Optional containing selected advertisement, or empty if list is empty
     */
    Optional<Advertisement> selectAd(List<Advertisement> candidates);
}
