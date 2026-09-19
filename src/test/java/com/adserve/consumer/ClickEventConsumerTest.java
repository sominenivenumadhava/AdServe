package com.adserve.consumer;

import com.adserve.entity.AdClick;
import com.adserve.event.AdClickEvent;
import com.adserve.repository.AdClickRepository;
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
class ClickEventConsumerTest {

    @Mock
    private AdClickRepository adClickRepository;

    @Mock
    private KafkaMetricsService kafkaMetricsService;

    @InjectMocks
    private ClickEventConsumer clickEventConsumer;

    @Test
    @DisplayName("consumeClick persists new click to MySQL and increments consumed metric")
    void testConsumeClickSuccess() {
        String eventId = "click-evt-999";
        AdClickEvent event = AdClickEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .campaignId(20L)
                .country("US")
                .device("IOS")
                .category("FINANCE")
                .timestamp(LocalDateTime.now())
                .build();

        when(adClickRepository.existsByEventId(eq(eventId))).thenReturn(false);
        when(adClickRepository.save(any(AdClick.class)))
                .thenAnswer(invocation -> {
                    AdClick saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        clickEventConsumer.consumeClick(event);

        ArgumentCaptor<AdClick> captor = ArgumentCaptor.forClass(AdClick.class);
        verify(adClickRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getAdId()).isEqualTo(101L);
        assertThat(captor.getValue().getCategory()).isEqualTo("FINANCE");

        verify(kafkaMetricsService, times(1)).incrementConsumedClicks();
        verify(kafkaMetricsService, never()).incrementDuplicateEventsIgnored();
    }

    @Test
    @DisplayName("Idempotency: duplicate eventId is skipped without inserting into MySQL")
    void testDuplicateClickEventIsSkipped() {
        String eventId = "CLICK_DUP_123";
        AdClickEvent event = AdClickEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .build();

        when(adClickRepository.existsByEventId(eq(eventId))).thenReturn(true);

        clickEventConsumer.consumeClick(event);

        verify(adClickRepository, never()).save(any(AdClick.class));
        verify(kafkaMetricsService, times(1)).incrementDuplicateEventsIgnored();
        verify(kafkaMetricsService, never()).incrementConsumedClicks();
    }

    @Test
    @DisplayName("Idempotency: DataIntegrityViolationException is caught and treated as duplicate")
    void testDataIntegrityViolationTreatedAsDuplicate() {
        String eventId = "CLICK_RACE_CONDITION";
        AdClickEvent event = AdClickEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .build();

        when(adClickRepository.existsByEventId(eq(eventId))).thenReturn(false);
        when(adClickRepository.save(any(AdClick.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key error"));

        clickEventConsumer.consumeClick(event);

        verify(kafkaMetricsService, times(1)).incrementDuplicateEventsIgnored();
        verify(kafkaMetricsService, never()).incrementConsumedClicks();
    }

    @Test
    @DisplayName("Consumer failure: unexpected exception triggers failure metric and rethrows for retry")
    void testConsumerFailureRethrowsForRetry() {
        String eventId = "CLICK_ERR_EVENT";
        AdClickEvent event = AdClickEvent.builder()
                .eventId(eventId)
                .adId(101L)
                .build();

        when(adClickRepository.existsByEventId(eq(eventId))).thenReturn(false);
        when(adClickRepository.save(any(AdClick.class)))
                .thenThrow(new RuntimeException("Database timeout"));

        assertThatThrownBy(() -> clickEventConsumer.consumeClick(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database timeout");

        verify(kafkaMetricsService, times(1)).incrementProcessingFailures();
    }
}
