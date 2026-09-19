package com.adserve.service;

import com.adserve.event.AdClickEvent;
import com.adserve.event.AdImpressionEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private KafkaMetricsService kafkaMetricsService;

    @InjectMocks
    private AdEventProducer adEventProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adEventProducer, "impressionsTopic", "ad-impressions");
        ReflectionTestUtils.setField(adEventProducer, "clicksTopic", "ad-clicks");
    }

    @Test
    @DisplayName("publishImpression sends event to 'ad-impressions' topic with adId as partition key")
    void testPublishImpressionSuccess() {
        AdImpressionEvent event = AdImpressionEvent.builder()
                .adId(101L)
                .campaignId(20L)
                .country("IN")
                .device("ANDROID")
                .category("GAMING")
                .build();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("ad-impressions", 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("ad-impressions"), eq("101"), any(AdImpressionEvent.class)))
                .thenReturn(future);

        CompletableFuture<SendResult<String, Object>> resultFuture = adEventProducer.publishImpression(event);

        assertThat(resultFuture).isNotNull();
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getTimestamp()).isNotNull();

        ArgumentCaptor<AdImpressionEvent> eventCaptor = ArgumentCaptor.forClass(AdImpressionEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("ad-impressions"), eq("101"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAdId()).isEqualTo(101L);
        assertThat(eventCaptor.getValue().getEventId()).isNotBlank();
        verify(kafkaMetricsService, times(1)).incrementPublishedImpressions();
    }

    @Test
    @DisplayName("publishClick sends event to 'ad-clicks' topic with adId as partition key")
    void testPublishClickSuccess() {
        AdClickEvent event = AdClickEvent.builder()
                .adId(101L)
                .campaignId(20L)
                .country("US")
                .device("IOS")
                .category("TECH")
                .build();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("ad-clicks", 1), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("ad-clicks"), eq("101"), any(AdClickEvent.class)))
                .thenReturn(future);

        CompletableFuture<SendResult<String, Object>> resultFuture = adEventProducer.publishClick(event);

        assertThat(resultFuture).isNotNull();
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getTimestamp()).isNotNull();

        ArgumentCaptor<AdClickEvent> eventCaptor = ArgumentCaptor.forClass(AdClickEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("ad-clicks"), eq("101"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAdId()).isEqualTo(101L);
        assertThat(eventCaptor.getValue().getEventId()).isNotBlank();
        verify(kafkaMetricsService, times(1)).incrementPublishedClicks();
    }

    @Test
    @DisplayName("publishImpression tracks failure when KafkaTemplate completes exceptionally")
    void testPublishImpressionFailure() {
        AdImpressionEvent event = AdImpressionEvent.builder()
                .adId(101L)
                .build();

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Broker connection timeout"));

        when(kafkaTemplate.send(eq("ad-impressions"), eq("101"), any(AdImpressionEvent.class)))
                .thenReturn(failedFuture);

        adEventProducer.publishImpression(event);

        verify(kafkaMetricsService, times(1)).incrementProcessingFailures();
        verify(kafkaMetricsService, never()).incrementPublishedImpressions();
    }
}
