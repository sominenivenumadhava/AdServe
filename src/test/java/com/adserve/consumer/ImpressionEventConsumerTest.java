package com.adserve.consumer;

import com.adserve.entity.AdImpression;
import com.adserve.event.AdImpressionEvent;
import com.adserve.repository.AdImpressionRepository;
import com.adserve.service.KafkaMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImpressionEventConsumerTest {

    @Mock
    private AdImpressionRepository adImpressionRepository;

    @Mock
    private KafkaMetricsService kafkaMetricsService;

    @InjectMocks
    private ImpressionEventConsumer impressionEventConsumer;

    @Test
    @DisplayName("consumeImpression persists new impression to MySQL and increments consumed metric")
    void testConsumeImpressionSuccess() {
        String eventId = "imp-evt-12345";
        AdImpressionEvent event = AdImpressionEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .campaignId(20L)
                .country("IN")
                .device("ANDROID")
                .category("GAMING")
                .timestamp(LocalDateTime.now())
                .build();

        when(adImpressionRepository.existsByEventId(eq(eventId))).thenReturn(false);
        when(adImpressionRepository.save(any(AdImpression.class)))
                .thenAnswer(invocation -> {
                    AdImpression saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        impressionEventConsumer.consumeImpression(event);

        ArgumentCaptor<AdImpression> captor = ArgumentCaptor.forClass(AdImpression.class);
        verify(adImpressionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getAdId()).isEqualTo(101L);
        assertThat(captor.getValue().getCountry()).isEqualTo("IN");

        verify(kafkaMetricsService, times(1)).incrementConsumedImpressions();
        verify(kafkaMetricsService, never()).incrementDuplicateEventsIgnored();
    }

    @Test
    @DisplayName("Idempotency: duplicate eventId is skipped without inserting into MySQL")
    void testDuplicateEventIdIsSkipped() {
        String eventId = "ABC123DUPLICATE";
        AdImpressionEvent event = AdImpressionEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .campaignId(20L)
                .build();

        when(adImpressionRepository.existsByEventId(eq(eventId))).thenReturn(true);

        impressionEventConsumer.consumeImpression(event);

        verify(adImpressionRepository, never()).save(any(AdImpression.class));
        verify(kafkaMetricsService, times(1)).incrementDuplicateEventsIgnored();
        verify(kafkaMetricsService, never()).incrementConsumedImpressions();
    }

    @Test
    @DisplayName("Idempotency: DataIntegrityViolationException is caught and treated as duplicate")
    void testDataIntegrityViolationTreatedAsDuplicate() {
        String eventId = "RACE_CONDITION_EVENT";
        AdImpressionEvent event = AdImpressionEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .build();

        when(adImpressionRepository.existsByEventId(eq(eventId))).thenReturn(false);
        when(adImpressionRepository.save(any(AdImpression.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key error"));

        impressionEventConsumer.consumeImpression(event);

        verify(kafkaMetricsService, times(1)).incrementDuplicateEventsIgnored();
        verify(kafkaMetricsService, never()).incrementConsumedImpressions();
    }

    @Test
    @DisplayName("Consumer failure: unexpected exception triggers failure metric and rethrows for retry")
    void testConsumerFailureRethrowsForRetry() {
        String eventId = "ERR_EVENT";
        AdImpressionEvent event = AdImpressionEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .build();

        when(adImpressionRepository.existsByEventId(eq(eventId))).thenReturn(false);
        when(adImpressionRepository.save(any(AdImpression.class)))
                .thenThrow(new RuntimeException("Database down"));

        assertThatThrownBy(() -> impressionEventConsumer.consumeImpression(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database down");

        verify(kafkaMetricsService, times(1)).incrementProcessingFailures();
    }
}
