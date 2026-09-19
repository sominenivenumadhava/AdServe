package com.adserve.consumer;

import com.adserve.entity.AdClick;
import com.adserve.event.AdClickEvent;
import com.adserve.repository.AdClickRepository;
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
 * Kafka consumer responsible for processing advertisement click events.
 * Belongs to consumer group 'click-analytics-group'.
 * Enforces strict idempotency via unique eventId verification before persisting to MySQL.
 */
@Component
@RequiredArgsConstructor
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final AdClickRepository adClickRepository;
    private final KafkaMetricsService kafkaMetricsService;

    /**
     * Consumes click events from 'ad-clicks' topic.
     * Checks if eventId already exists in MySQL. If exists, ignores the message (idempotent).
     * Otherwise, inserts the record and increments consumed metrics.
     *
     * @param event The click event received from Kafka
     */
    @KafkaListener(
            topics = "${adserve.kafka.topics.clicks:ad-clicks}",
            groupId = "${adserve.kafka.consumer.group.click:click-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeClick(AdClickEvent event) {
        String eventId = event.getEventId();
        Long adId = event.getAdId();

        log.info("Received click event: eventId={}, adId={}, timestamp={}",
                eventId, adId, event.getTimestamp());

        // Idempotency check: Does eventId already exist in MySQL?
        if (eventId != null && adClickRepository.existsByEventId(eventId)) {
            log.warn("Duplicate click event detected for eventId: {}. Skipping insertion to maintain idempotency.", eventId);
            kafkaMetricsService.incrementDuplicateEventsIgnored();
            return;
        }

        try {
            AdClick click = AdClick.builder()
                    .eventId(eventId)
                    .adId(adId)
                    .campaignId(event.getCampaignId())
                    .country(event.getCountry())
                    .device(event.getDevice())
                    .category(event.getCategory())
                    .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                    .build();

            AdClick saved = adClickRepository.save(click);
            kafkaMetricsService.incrementConsumedClicks();

            log.info("Successfully persisted click to MySQL: clickId={}, eventId={}, adId={}",
                    saved.getId(), eventId, adId);

        } catch (DataIntegrityViolationException e) {
            // Catches any race-condition duplicate inserts matching uk_click_event_id
            log.warn("Database unique constraint violation for eventId: {}. Treating as duplicate event.", eventId);
            kafkaMetricsService.incrementDuplicateEventsIgnored();
        } catch (Exception e) {
            log.error("Failed to process and persist click event [{}]: {}", eventId, e.getMessage(), e);
            kafkaMetricsService.incrementProcessingFailures();
            throw e; // Rethrow to trigger container retry policy
        }
    }
}
