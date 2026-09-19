package com.adserve.repository;

import com.adserve.entity.AdStatus;
import com.adserve.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Advertisement creative entity.
 */
@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    List<Advertisement> findByCampaignId(Long campaignId);

    List<Advertisement> findByStatus(AdStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Advertisement a JOIN FETCH a.campaign c JOIN FETCH c.advertiser WHERE a.status = :status")
    List<Advertisement> findAllWithCampaignByStatus(@org.springframework.data.repository.query.Param("status") AdStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Advertisement a WHERE a.campaign.advertiser.id = :advertiserId")
    List<Advertisement> findByAdvertiserId(@org.springframework.data.repository.query.Param("advertiserId") Long advertiserId);
}
