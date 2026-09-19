package com.adserve.repository;

import com.adserve.entity.AdClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for AdClick entity.
 */
@Repository
public interface AdClickRepository extends JpaRepository<AdClick, Long> {

    long countByAdId(Long adId);

    boolean existsByEventId(String eventId);

    java.util.Optional<AdClick> findByEventId(String eventId);
}
