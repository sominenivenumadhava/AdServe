package com.adserve.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event model representing an advertisement impression.
 * Serialized as JSON and published to the 'ad-impressions' Kafka topic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdImpressionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for idempotency checking and deduplication.
     */
    private String eventId;

    /**
     * Identifier of the served advertisement.
     */
    private Long adId;

    /**
     * Identifier of the campaign associated with the advertisement.
     */
    private Long campaignId;

    /**
     * ISO-8601 timestamp when the impression occurred.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Client targeting context: Country code (e.g., IN, US, UK).
     */
    private String country;

    /**
     * Client targeting context: Device type (e.g., MOBILE, DESKTOP, ANDROID, IOS).
     */
    private String device;

    /**
     * Client targeting context: Content category (e.g., GAMING, TECH, FINANCE).
     */
    private String category;
}
