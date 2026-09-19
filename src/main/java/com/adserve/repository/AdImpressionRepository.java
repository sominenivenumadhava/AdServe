package com.adserve.repository;

import com.adserve.entity.AdImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for AdImpression entity.
 */
@Repository
public interface AdImpressionRepository extends JpaRepository<AdImpression, Long> {

    long countByAdId(Long adId);

    boolean existsByEventId(String eventId);

    java.util.Optional<AdImpression> findByEventId(String eventId);
}
