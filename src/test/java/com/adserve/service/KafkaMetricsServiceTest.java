package com.adserve.service;

import com.adserve.dto.KafkaStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaMetricsServiceTest {

    private KafkaMetricsService kafkaMetricsService;

    @BeforeEach
    void setUp() {
        kafkaMetricsService = new KafkaMetricsService();
    }

    @Test
    @DisplayName("KafkaMetricsService correctly increments and tracks all streaming metrics")
    void testMetricsTracking() {
        kafkaMetricsService.incrementPublishedImpressions();
        kafkaMetricsService.incrementPublishedImpressions();
        kafkaMetricsService.incrementPublishedClicks();

        kafkaMetricsService.incrementConsumedImpressions();
        kafkaMetricsService.incrementConsumedClicks();

        kafkaMetricsService.incrementDuplicateEventsIgnored();
        kafkaMetricsService.incrementProcessingFailures();

        assertThat(kafkaMetricsService.getPublishedImpressions()).isEqualTo(2);
        assertThat(kafkaMetricsService.getPublishedClicks()).isEqualTo(1);
        assertThat(kafkaMetricsService.getConsumedImpressions()).isEqualTo(1);
        assertThat(kafkaMetricsService.getConsumedClicks()).isEqualTo(1);
        assertThat(kafkaMetricsService.getDuplicateEventsIgnored()).isEqualTo(1);
        assertThat(kafkaMetricsService.getProcessingFailures()).isEqualTo(1);

        KafkaStatusDto status = kafkaMetricsService.getStatus();
        assertThat(status).isNotNull();
        assertThat(status.getPublishedImpressions()).isEqualTo(2);
        assertThat(status.getPublishedClicks()).isEqualTo(1);
        assertThat(status.getConsumedImpressions()).isEqualTo(1);
        assertThat(status.getConsumedClicks()).isEqualTo(1);
        assertThat(status.getDuplicateEventsIgnored()).isEqualTo(1);
        assertThat(status.getProcessingFailures()).isEqualTo(1);
    }

    @Test
    @DisplayName("reset clears all accumulated metrics")
    void testReset() {
        kafkaMetricsService.incrementPublishedImpressions();
        kafkaMetricsService.incrementConsumedImpressions();
        kafkaMetricsService.reset();

        assertThat(kafkaMetricsService.getPublishedImpressions()).isZero();
        assertThat(kafkaMetricsService.getConsumedImpressions()).isZero();
    }
}
