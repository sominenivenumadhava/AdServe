package com.adserve.consumer;

import com.adserve.entity.AdImpression;
import com.adserve.event.AdImpressionEvent;
import com.adserve.repository.AdImpressionRepository;
import com.adserve.service.KafkaMetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka consumer responsible for processing advertisement impression events.
 * Belongs to consumer group 'impression-analytics-group'.
 * Enforces strict idempotency via unique eventId verification before persisting to MySQL.
 */
@Component
@RequiredArgsConstructor
public class ImpressionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ImpressionEventConsumer.class);

    private final AdImpressionRepository adImpressionRepository;
    private final KafkaMetricsService kafkaMetricsService;

    /**
     * Consumes impression events from 'ad-impressions' topic.
     * Checks if eventId already exists in MySQL. If exists, ignores the message (idempotent).
     * Otherwise, inserts the record and increments consumed metrics.
     *
     * @param event The impression event received from Kafka
     */
    @KafkaListener(
            topics = "${adserve.kafka.topics.impressions:ad-impressions}",
            groupId = "${adserve.kafka.consumer.group.impression:impression-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeImpression(AdImpressionEvent event) {
        String eventId = event.getEventId();
        Long adId = event.getAdId();

        log.info("Received impression event: eventId={}, adId={}, timestamp={}",
                eventId, adId, event.getTimestamp());

        // Idempotency check: Does eventId already exist in MySQL?
        if (eventId != null && adImpressionRepository.existsByEventId(eventId)) {
            log.warn("Duplicate impression event detected for eventId: {}. Skipping insertion to maintain idempotency.", eventId);
            kafkaMetricsService.incrementDuplicateEventsIgnored();
            return;
        }

        try {
            AdImpression impression = AdImpression.builder()
                    .eventId(eventId)
                    .adId(adId)
                    .campaignId(event.getCampaignId())
                    .country(event.getCountry())
                    .device(event.getDevice())
                    .category(event.getCategory())
                    .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                    .build();

            AdImpression saved = adImpressionRepository.save(impression);
            kafkaMetricsService.incrementConsumedImpressions();

            log.info("Successfully persisted impression to MySQL: impressionId={}, eventId={}, adId={}",
                    saved.getId(), eventId, adId);

        } catch (DataIntegrityViolationException e) {
            // Catches any race-condition duplicate inserts matching uk_impression_event_id
            log.warn("Database unique constraint violation for eventId: {}. Treating as duplicate event.", eventId);
            kafkaMetricsService.incrementDuplicateEventsIgnored();
        } catch (Exception e) {
            log.error("Failed to process and persist impression event [{}]: {}", eventId, e.getMessage(), e);
            kafkaMetricsService.incrementProcessingFailures();
            throw e; // Rethrow to trigger container retry policy
        }
    }
}
