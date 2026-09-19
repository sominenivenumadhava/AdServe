package com.adserve.repository;

import com.adserve.entity.Campaign;
import com.adserve.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Campaign entity.
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByAdvertiserId(Long advertiserId);

    List<Campaign> findByStatus(CampaignStatus status);
}
