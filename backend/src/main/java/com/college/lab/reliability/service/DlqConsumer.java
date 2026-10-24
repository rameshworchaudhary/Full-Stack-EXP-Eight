package com.college.lab.reliability.service;

import com.college.lab.reliability.entity.DlqRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.repository.DlqRecordRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqRecordRepository dlqRecordRepository;
    private final EventLogService eventLogService;
    private final EventRecordRepository eventRecordRepository;

    public DlqConsumer(DlqRecordRepository dlqRecordRepository, EventLogService eventLogService,
                       EventRecordRepository eventRecordRepository) {
        this.dlqRecordRepository = dlqRecordRepository;
        this.eventLogService = eventLogService;
        this.eventRecordRepository = eventRecordRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.dlq:events-dlq}",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeDlq(ConsumerRecord<String, String> record) {
        String eventId = record.key();
        log.warn("[DLQ-CONSUMER] Message received in DLQ: eventId='{}', partition={}, offset={}",
                eventId, record.partition(), record.offset());

        // Extract metadata from Kafka headers populated by DeadLetterPublishingRecoverer
        Map<String, String> headerMap = new HashMap<>();
        String originalTopic = null;
        Integer originalPartition = null;
        Long originalOffset = null;
        String failureReason = "Retries exhausted";

        for (Header header : record.headers()) {
            String key = header.key();
            String val = new String(header.value(), StandardCharsets.UTF_8);
            headerMap.put(key, val);

            if ("kafka_dlt-original-topic".equalsIgnoreCase(key)) {
                originalTopic = val;
            } else if ("kafka_dlt-original-partition".equalsIgnoreCase(key)) {
                try {
                    originalPartition = Integer.parseInt(val);
                } catch (NumberFormatException ignored) {}
            } else if ("kafka_dlt-original-offset".equalsIgnoreCase(key)) {
                try {
                    originalOffset = Long.parseLong(val);
                } catch (NumberFormatException ignored) {}
            } else if ("kafka_dlt-exception-message".equalsIgnoreCase(key)) {
                failureReason = val;
            }
        }

        // Persist DLQ record
        DlqRecord dlqRecord = new DlqRecord(
                eventId != null ? eventId : "UNKNOWN",
                originalTopic,
                originalPartition,
                originalOffset,
                record.value(),
                failureReason,
                headerMap.toString()
        );
        int attemptsMade = eventId == null ? 0 : eventRecordRepository.findByEventId(eventId)
                .map(eventRecord -> eventRecord.getAttemptCount()).orElse(0);
        dlqRecord.setAttemptsMade(attemptsMade);
        dlqRecordRepository.save(dlqRecord);

        // Update event record status and log DLQ arrival
        if (eventId != null) {
            eventLogService.updateEventStatus(eventId, EventStatus.DLQ, attemptsMade);
            eventLogService.recordLog(
                    eventId,
                    attemptsMade,
                    "DLQ_ROUTED",
                    0L,
                    "Event routed to Dead Letter Queue after all retry attempts were exhausted. Reason: " + failureReason
            );
        }

        log.info("[DLQ-PERSISTED] Saved DLQ record for event '{}' with ID {}", eventId, dlqRecord.getId());
    }
}
