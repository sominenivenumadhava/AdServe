package com.adserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing Kafka streaming health and event-processing metrics.
 * Exposed via GET /api/system/kafka-status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaStatusDto {

    /**
     * Overall Kafka broker connectivity status: UP, DOWN, or DISABLED.
     */
    private String kafka;

    /**
     * Impression producer status (UP / DOWN).
     */
    private String impressionProducer;

    /**
     * Click producer status (UP / DOWN).
     */
    private String clickProducer;

    /**
     * Consumer listener status (UP / DOWN).
     */
    private String consumers;

    /**
     * Total advertisement impression events dispatched to 'ad-impressions'.
     */
    private long publishedImpressions;

    /**
     * Total advertisement click events dispatched to 'ad-clicks'.
     */
    private long publishedClicks;

    /**
     * Total impression events successfully consumed and persisted.
     */
    private long consumedImpressions;

    /**
     * Total click events successfully consumed and persisted.
     */
    private long consumedClicks;

    /**
     * Total duplicate events identified and skipped via idempotency check.
     */
    private long duplicateEventsIgnored;

    /**
     * Total event processing errors or deserialization failures.
     */
    private long processingFailures;
}
