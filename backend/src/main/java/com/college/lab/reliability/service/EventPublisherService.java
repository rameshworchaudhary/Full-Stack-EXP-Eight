package com.college.lab.reliability.service;

import com.college.lab.reliability.dto.EventMessage;
import com.college.lab.reliability.dto.PublishEventRequest;
import com.college.lab.reliability.entity.EventLog;
import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.repository.EventLogRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import com.college.lab.reliability.entity.FailureMode;
import com.college.lab.reliability.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventRecordRepository eventRecordRepository;
    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;
    private final String eventsTopic;


    public EventPublisherService(
            KafkaTemplate<String, String> kafkaTemplate,
            EventRecordRepository eventRecordRepository,
            EventLogRepository eventLogRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.events:events-topic}") String eventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventRecordRepository = eventRecordRepository;
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
        this.eventsTopic = eventsTopic;
    }

    @Transactional
    public EventRecord publishEvent(PublishEventRequest request) {
        log.info("Publishing event: id={}, type={}, mode={}",
                request.getEventId(), request.getEventType(), request.getFailureMode());

        EventRecord record = eventRecordRepository.findByEventId(request.getEventId())
                .orElseGet(() -> new EventRecord(
                        request.getEventId(),
                        request.getEventType(),
                        request.getPayload(),
                        request.getFailureMode(),
                        EventStatus.SUBMITTED
                ));

        // If it already exists (e.g. resubmitting for idempotency test), update metadata but preserve history
        record.setEventType(request.getEventType());
        record.setPayload(request.getPayload());
        record.setFailureMode(request.getFailureMode());
        if (record.getStatus() != EventStatus.SUCCESS) {
            record.setStatus(EventStatus.SUBMITTED);
        }
        EventRecord savedRecord = eventRecordRepository.save(record);

        // Record submission log
        eventLogRepository.save(new EventLog(
                request.getEventId(),
                record.getAttemptCount(),
                "SUBMITTED",
                0L,
                "Event published to Kafka topic '" + eventsTopic + "' with failure mode: " + request.getFailureMode()
        ));

        // Serialize and publish to Kafka
        try {
            EventMessage message = new EventMessage(
                    request.getEventId(),
                    request.getEventType(),
                    request.getPayload(),
                    request.getFailureMode()
            );
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(eventsTopic, request.getEventId(), messageJson);
            log.info("Successfully sent message for event '{}' to topic '{}'", request.getEventId(), eventsTopic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event message for eventId: {}", request.getEventId(), e);
            throw new RuntimeException("Failed to serialize event message: " + e.getMessage(), e);
        }

        return savedRecord;
    }

    @Transactional
    public EventRecord replayDlqEvent(String eventId) {
        log.info("[DLQ-REPLAY] Request received to replay DLQ event: {}", eventId);
        EventRecord record = eventRecordRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot replay: Event not found with eventId: " + eventId));

        // Update record
        record.setFailureMode(FailureMode.NONE);
        record.setStatus(EventStatus.SUBMITTED);
        EventRecord savedRecord = eventRecordRepository.save(record);

        // Log replay initiation
        eventLogRepository.save(new EventLog(
                eventId,
                record.getAttemptCount(),
                "DLQ_REPLAY_REQUESTED",
                0L,
                "DLQ event re-queued for processing with resolved failure mode: NONE."
        ));

        // Re-publish to Kafka events-topic
        try {
            EventMessage message = new EventMessage(
                    record.getEventId(),
                    record.getEventType(),
                    record.getPayload(),
                    FailureMode.NONE
            );
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(eventsTopic, record.getEventId(), messageJson);
            log.info("[DLQ-REPLAY] Successfully re-published DLQ event '{}' to topic '{}'", eventId, eventsTopic);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize replayed event message: " + e.getMessage(), e);
        }

        return savedRecord;
    }
}
