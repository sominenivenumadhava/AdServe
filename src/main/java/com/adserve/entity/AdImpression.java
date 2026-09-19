package com.adserve.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity recording an advertisement impression (when an ad is rendered/served).
 */
@Entity
@Table(name = "ad_impressions", indexes = {
        @Index(name = "idx_impression_ad_id", columnList = "ad_id"),
        @Index(name = "idx_impression_timestamp", columnList = "timestamp"),
        @Index(name = "idx_impression_event_id", columnList = "event_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_impression_event_id", columnNames = {"event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, length = 64)
    private String eventId;

    @Column(name = "ad_id", nullable = false)
    private Long adId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "country", length = 32)
    private String country;

    @Column(name = "device", length = 32)
    private String device;

    @Column(name = "category", length = 64)
    private String category;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
