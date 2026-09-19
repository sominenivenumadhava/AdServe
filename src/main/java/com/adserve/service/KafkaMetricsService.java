package com.adserve.service;

import com.adserve.dto.KafkaStatusDto;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service tracking Kafka streaming metrics and cluster connectivity.
 * Thread-safe counters record publications, consumptions, idempotency deductions, and failures.
 */
@Service
public class KafkaMetricsService {

    private static final Logger log = LoggerFactory.getLogger(KafkaMetricsService.class);

    private final AtomicLong publishedImpressions = new AtomicLong(0);
    private final AtomicLong publishedClicks = new AtomicLong(0);
    private final AtomicLong consumedImpressions = new AtomicLong(0);
    private final AtomicLong consumedClicks = new AtomicLong(0);
    private final AtomicLong duplicateEventsIgnored = new AtomicLong(0);
    private final AtomicLong processingFailures = new AtomicLong(0);

    @Autowired(required = false)
    private KafkaAdmin kafkaAdmin;

    public void incrementPublishedImpressions() {
        publishedImpressions.incrementAndGet();
    }

    public void incrementPublishedClicks() {
        publishedClicks.incrementAndGet();
    }

    public void incrementConsumedImpressions() {
        consumedImpressions.incrementAndGet();
    }

    public void incrementConsumedClicks() {
        consumedClicks.incrementAndGet();
    }

    public void incrementDuplicateEventsIgnored() {
        duplicateEventsIgnored.incrementAndGet();
    }

    public void incrementProcessingFailures() {
        processingFailures.incrementAndGet();
    }

    public long getPublishedImpressions() {
        return publishedImpressions.get();
    }

    public long getPublishedClicks() {
        return publishedClicks.get();
    }

    public long getConsumedImpressions() {
        return consumedImpressions.get();
    }

    public long getConsumedClicks() {
        return consumedClicks.get();
    }

    public long getDuplicateEventsIgnored() {
        return duplicateEventsIgnored.get();
    }

    public long getProcessingFailures() {
        return processingFailures.get();
    }

    public void reset() {
        publishedImpressions.set(0);
        publishedClicks.set(0);
        consumedImpressions.set(0);
        consumedClicks.set(0);
        duplicateEventsIgnored.set(0);
        processingFailures.set(0);
    }

    /**
     * Checks Kafka cluster connectivity with a short timeout.
     */
    public boolean isKafkaAvailable() {
        if (kafkaAdmin == null) {
            return false;
        }
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult result = client.describeCluster();
            result.clusterId().get(1500, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            log.debug("Kafka broker is unreachable or check timed out: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gathers real-time streaming health and event processing counters.
     */
    public KafkaStatusDto getStatus() {
        boolean available = isKafkaAvailable();
        String clusterStatus = available ? "UP" : "DOWN";

        return KafkaStatusDto.builder()
                .kafka(clusterStatus)
                .impressionProducer(available ? "UP" : "DOWN")
                .clickProducer(available ? "UP" : "DOWN")
                .consumers(available ? "UP" : "DOWN")
                .publishedImpressions(publishedImpressions.get())
                .publishedClicks(publishedClicks.get())
                .consumedImpressions(consumedImpressions.get())
                .consumedClicks(consumedClicks.get())
                .duplicateEventsIgnored(duplicateEventsIgnored.get())
                .processingFailures(processingFailures.get())
                .build();
    }
}
