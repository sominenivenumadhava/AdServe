package com.adserve.config;

import com.adserve.service.KafkaMetricsService;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration class.
 * Configures:
 * 1. Topic definitions ('ad-impressions', 'ad-clicks') with 3 partitions and 1 replica.
 * 2. ProducerFactory and KafkaTemplate with JSON serialization.
 * 3. ConsumerFactory and ConcurrentKafkaListenerContainerFactory with trusted deserializers.
 * 4. Resilient error handler with exponential/fixed backoff retry.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${adserve.kafka.topics.impressions:ad-impressions}")
    private String impressionsTopic;

    @Value("${adserve.kafka.topics.clicks:ad-clicks}")
    private String clicksTopic;

    @Value("${adserve.kafka.topics.partitions:3}")
    private int topicPartitions;

    @Value("${adserve.kafka.topics.replicas:1}")
    private int topicReplicas;

    /**
     * KafkaAdmin bean configured not to block application startup if broker is unavailable.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(true);
        return admin;
    }

    /**
     * Topic for advertisement impression events.
     * Configured with 3 partitions for consumer horizontal scalability.
     */
    @Bean
    public NewTopic adImpressionsTopic() {
        return TopicBuilder.name(impressionsTopic)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    /**
     * Topic for advertisement click events.
     * Configured with 3 partitions for consumer horizontal scalability.
     */
    @Bean
    public NewTopic adClicksTopic() {
        return TopicBuilder.name(clicksTopic)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    /**
     * Dead-letter topic for ad-impressions failures.
     */
    @Bean
    public NewTopic adImpressionsDltTopic() {
        return TopicBuilder.name(impressionsTopic + ".DLT")
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    /**
     * Dead-letter topic for ad-clicks failures.
     */
    @Bean
    public NewTopic adClicksDltTopic() {
        return TopicBuilder.name(clicksTopic + ".DLT")
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    /**
     * Producer configuration with JSON value serializer.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Consumer configuration with JSON value deserializer and all packages trusted.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Concurrent listener container factory with robust error handler.
     * Retries failed messages twice with 1000ms backoff before logging and recording failure.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaMetricsService kafkaMetricsService) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Simple retry mechanism: 2 retries every 1 second
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            log.error("Kafka consumer exhausted all retries for topic: {}, partition: {}, offset: {}. Error: {}",
                    record.topic(), record.partition(), record.offset(), exception.getMessage());
            kafkaMetricsService.incrementProcessingFailures();
        }, new FixedBackOff(1000L, 2L));

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
