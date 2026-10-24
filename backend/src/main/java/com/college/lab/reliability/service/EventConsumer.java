package com.college.lab.reliability.service;

import com.college.lab.reliability.dto.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final EventProcessingService eventProcessingService;
    private final ObjectMapper objectMapper;

    public EventConsumer(EventProcessingService eventProcessingService, ObjectMapper objectMapper) {
        this.eventProcessingService = eventProcessingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.events:events-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[CONSUMER] Received record from topic '{}', partition {}, offset {}, key '{}'",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            EventMessage message = objectMapper.readValue(record.value(), EventMessage.class);
            eventProcessingService.processEvent(message);
        } catch (Exception ex) {
            log.error("[CONSUMER-ERROR] Error handling message for key '{}': {}", record.key(), ex.getMessage());
            // Re-throw so Spring Kafka DefaultErrorHandler handles retry backoff or DLQ recovery
            throw new RuntimeException(ex);
        }
    }
}
