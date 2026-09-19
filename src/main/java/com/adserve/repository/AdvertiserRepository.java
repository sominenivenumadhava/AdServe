package com.adserve.repository;

import com.adserve.entity.Advertiser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Advertiser entity.
 */
@Repository
public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {

    boolean existsByEmail(String email);

    Optional<Advertiser> findByEmail(String email);

    Optional<Advertiser> findByUserId(Long userId);
}
