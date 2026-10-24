package com.college.lab.reliability.config;

import com.college.lab.reliability.exception.DuplicateEventException;
import com.college.lab.reliability.exception.PermanentProcessingException;
import com.college.lab.reliability.exception.TransientProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${app.kafka.topics.events:events-topic}")
    private String eventsTopic;

    @Value("${app.kafka.topics.dlq:events-dlq}")
    private String dlqTopic;

    @Value("${app.kafka.retry.max-attempts:4}")
    private int maxAttempts;

    @Value("${app.kafka.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${app.kafka.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${app.kafka.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    @Bean
    public NewTopic eventsTopicBean() {
        return TopicBuilder.name(eventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopicBean() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Configures the Dead Letter Queue recoverer which routes failed records
     * to the 'events-dlq' topic once retries are exhausted.
     */
    @Bean
    public ConsumerRecordRecoverer deadLetterRecoverer(KafkaOperations<Object, Object> template) {
        return new DeadLetterPublishingRecoverer(template,
                (record, ex) -> {
                    log.error("[DLQ-RECOVERER] Retries exhausted for record key='{}'. Routing to DLQ topic '{}'. Cause: {}",
                            record.key(), dlqTopic, ex.getMessage());
                    return new TopicPartition(dlqTopic, -1);
                });
    }

    /**
     * Default error handler implementing initial attempt + 3 retries (4 total attempts)
     * with exponential backoff and routing to Dead Letter Queue.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(ConsumerRecordRecoverer deadLetterRecoverer) {
        // maxRetries = maxAttempts - 1 (e.g. 4 - 1 = 3 retries)
        int maxRetries = Math.max(0, maxAttempts - 1);
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxIntervalMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterRecoverer, backOff);

        // Exceptions to retry
        errorHandler.addRetryableExceptions(TransientProcessingException.class);
        errorHandler.addRetryableExceptions(PermanentProcessingException.class);

        // Non-retryable
        errorHandler.addNotRetryableExceptions(DuplicateEventException.class);

        // Listener to observe and log each retry attempt
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("[RETRY-LISTENER] Retry attempt #{} for event key='{}' on topic '{}'. Error: {}",
                    deliveryAttempt, record.key(), record.topic(), ex.getMessage());
        });

        return errorHandler;
    }

    /**
     * Main container factory for event consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * Dedicated container factory for the DLQ consumer.
     * Prevents infinite DLQ loops by not recovering back into DLQ.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Fixed backoff 0 retries so DLQ never loops
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
        return factory;
    }
}
