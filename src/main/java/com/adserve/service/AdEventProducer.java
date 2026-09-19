package com.adserve.service;

import com.adserve.event.AdClickEvent;
import com.adserve.event.AdImpressionEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service responsible for dispatching advertisement interaction events
 * to their respective topics ('ad-impressions' and 'ad-clicks').
 * Operates non-blockingly to guarantee minimal latency on the HTTP request path.
 */
@Service
@RequiredArgsConstructor
public class AdEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AdEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaMetricsService kafkaMetricsService;

    @Value("${adserve.kafka.topics.impressions:ad-impressions}")
    private String impressionsTopic;

    @Value("${adserve.kafka.topics.clicks:ad-clicks}")
    private String clicksTopic;

    /**
     * Publishes an advertisement impression event to the 'ad-impressions' topic.
     * Uses adId as the partition key to guarantee ordered processing per ad creative.
     *
     * @param event The impression event payload
     * @return CompletableFuture of SendResult
     */
    public CompletableFuture<SendResult<String, Object>> publishImpression(AdImpressionEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        String key = String.valueOf(event.getAdId());
        log.debug("Publishing impression event [{}] for adId {} to topic {}", event.getEventId(), key, impressionsTopic);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(impressionsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                kafkaMetricsService.incrementPublishedImpressions();
                log.info("Successfully published impression event [{}] to topic {} (partition: {}, offset: {})",
                        event.getEventId(),
                        impressionsTopic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                kafkaMetricsService.incrementProcessingFailures();
                log.error("Failed to publish impression event [{}] for adId {}: {}",
                        event.getEventId(), event.getAdId(), ex.getMessage());
            }
        });

        return future;
    }

    /**
     * Publishes an advertisement click event to the 'ad-clicks' topic.
     * Uses adId as the partition key to preserve event stream ordering.
     *
     * @param event The click event payload
     * @return CompletableFuture of SendResult
     */
    public CompletableFuture<SendResult<String, Object>> publishClick(AdClickEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        String key = String.valueOf(event.getAdId());
        log.debug("Publishing click event [{}] for adId {} to topic {}", event.getEventId(), key, clicksTopic);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(clicksTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                kafkaMetricsService.incrementPublishedClicks();
                log.info("Successfully published click event [{}] to topic {} (partition: {}, offset: {})",
                        event.getEventId(),
                        clicksTopic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                kafkaMetricsService.incrementProcessingFailures();
                log.error("Failed to publish click event [{}] for adId {}: {}",
                        event.getEventId(), event.getAdId(), ex.getMessage());
            }
        });

        return future;
    }
}
