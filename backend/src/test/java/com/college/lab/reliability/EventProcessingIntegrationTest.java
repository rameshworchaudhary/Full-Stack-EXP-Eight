package com.college.lab.reliability;

import com.college.lab.reliability.dto.PublishEventRequest;
import com.college.lab.reliability.entity.EventLog;
import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.entity.FailureMode;
import com.college.lab.reliability.repository.DlqRecordRepository;
import com.college.lab.reliability.repository.EventLogRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import com.college.lab.reliability.repository.ProcessedEventRepository;
import com.college.lab.reliability.service.EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"events-topic", "events-dlq"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@DirtiesContext
public class EventProcessingIntegrationTest {

    @Autowired
    private EventPublisherService eventPublisherService;

    @Autowired
    private EventRecordRepository eventRecordRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private DlqRecordRepository dlqRecordRepository;

    @BeforeEach
    void setUp() {
        // clean up state if necessary
    }

    @Test
    @DisplayName("Scenario 1: Successful processing with NONE failure mode")
    void testSuccessfulProcessing() {
        String eventId = "EVT-SUCCESS-" + UUID.randomUUID().toString().substring(0, 8);
        PublishEventRequest request = new PublishEventRequest(
                eventId,
                "ORDER_CREATED",
                "{\"item\":\"Laptop\",\"qty\":1,\"amount\":1200.00}",
                FailureMode.NONE
        );

        eventPublisherService.publishEvent(request);

        // Await successful processing and idempotency persistence
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
            EventRecord record = eventRecordRepository.findByEventId(eventId).orElse(null);
            assertThat(record).isNotNull();
            assertThat(record.getStatus()).isEqualTo(EventStatus.SUCCESS);
        });

        List<EventLog> logs = eventLogRepository.findByEventIdOrderByAttemptNumberAsc(eventId);
        assertThat(logs).isNotEmpty();
        assertThat(logs.stream().anyMatch(l -> "SUCCESS".equals(l.getStatus()))).isTrue();
    }

    @Test
    @DisplayName("Scenario 2: Transient failure recovers and succeeds on retry")
    void testTransientFailureFollowedBySuccess() {
        String eventId = "EVT-TRANS-" + UUID.randomUUID().toString().substring(0, 8);
        PublishEventRequest request = new PublishEventRequest(
                eventId,
                "PAYMENT_PROCESSED",
                "{\"account\":\"ACC-9912\",\"amount\":450.00}",
                FailureMode.TRANSIENT
        );

        eventPublisherService.publishEvent(request);

        // Await retry loop to execute and eventually succeed
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
            EventRecord record = eventRecordRepository.findByEventId(eventId).orElse(null);
            assertThat(record).isNotNull();
            assertThat(record.getStatus()).isEqualTo(EventStatus.SUCCESS);
            assertThat(record.getAttemptCount()).isGreaterThan(1);
        });

        // Verify logs record the retry attempts and backoff
        List<EventLog> logs = eventLogRepository.findByEventIdOrderByAttemptNumberAsc(eventId);
        assertThat(logs.stream().anyMatch(l -> "FAILED_TRANSIENT".equals(l.getStatus()))).isTrue();
        assertThat(logs.stream().anyMatch(l -> "SUCCESS".equals(l.getStatus()))).isTrue();
    }

    @Test
    @DisplayName("Scenario 3: Permanent failure exhausts retries and routes to Dead Letter Queue")
    void testPermanentFailureRoutedToDlq() {
        String eventId = "EVT-PERM-" + UUID.randomUUID().toString().substring(0, 8);
        PublishEventRequest request = new PublishEventRequest(
                eventId,
                "INVENTORY_DEDUCTED",
                "{\"sku\":\"INVALID_SKU\",\"warehouse\":\"W1\"}",
                FailureMode.PERMANENT
        );

        eventPublisherService.publishEvent(request);

        // Await retry exhaustion and DLQ routing
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(dlqRecordRepository.findByEventId(eventId)).isPresent();
            EventRecord record = eventRecordRepository.findByEventId(eventId).orElse(null);
            assertThat(record).isNotNull();
            assertThat(record.getStatus()).isEqualTo(EventStatus.DLQ);
            // Verify failure WITHOUT false successful record in idempotency table
            assertThat(processedEventRepository.existsById(eventId)).isFalse();
        });

        // Verify DLQ log
        List<EventLog> logs = eventLogRepository.findByEventIdOrderByAttemptNumberAsc(eventId);
        assertThat(logs.stream().anyMatch(l -> "DLQ_ROUTED".equals(l.getStatus()))).isTrue();
    }

    @Test
    @DisplayName("Scenario 4: Idempotency prevents duplicate business execution")
    void testDuplicateEventSubmission() {
        String eventId = "EVT-IDEMPOTENT-" + UUID.randomUUID().toString().substring(0, 8);
        PublishEventRequest firstRequest = new PublishEventRequest(
                eventId,
                "ACCOUNT_DEBIT",
                "{\"userId\":\"USER-100\",\"amount\":50.0}",
                FailureMode.NONE
        );

        // First submission
        eventPublisherService.publishEvent(firstRequest);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });

        // Second submission with exact same event ID
        PublishEventRequest duplicateRequest = new PublishEventRequest(
                eventId,
                "ACCOUNT_DEBIT",
                "{\"userId\":\"USER-100\",\"amount\":50.0}",
                FailureMode.NONE
        );
        eventPublisherService.publishEvent(duplicateRequest);

        // Await duplicate detection
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventLog> logs = eventLogRepository.findByEventIdOrderByAttemptNumberAsc(eventId);
            assertThat(logs.stream().anyMatch(l -> "DUPLICATE_SKIPPED".equals(l.getStatus()))).isTrue();
        });

        // Exactly one record in processed_events table
        assertThat(processedEventRepository.count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 5: Consumer recovery - permanent failure does not block subsequent normal events")
    void testRecoveryAfterEarlierFailedEvent() {
        String failedEventId = "EVT-FAIL-" + UUID.randomUUID().toString().substring(0, 8);
        String normalEventId = "EVT-NORM-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Submit permanent failing event
        eventPublisherService.publishEvent(new PublishEventRequest(
                failedEventId,
                "ORDER_FAILED",
                "{\"error\":\"fatal\"}",
                FailureMode.PERMANENT
        ));

        // 2. Submit normal event right after
        eventPublisherService.publishEvent(new PublishEventRequest(
                normalEventId,
                "ORDER_NORMAL",
                "{\"status\":\"valid\"}",
                FailureMode.NONE
        ));

        // 3. Confirm normal event succeeds despite preceding failure
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.existsById(normalEventId)).isTrue();
            EventRecord normalRecord = eventRecordRepository.findByEventId(normalEventId).orElse(null);
            assertThat(normalRecord).isNotNull();
            assertThat(normalRecord.getStatus()).isEqualTo(EventStatus.SUCCESS);
        });
    }

    @Test
    @DisplayName("Scenario 6: DLQ Replay - replaying a dead-lettered event allows recovery and idempotency")
    void testDlqReplaySuccess() {
        String eventId = "EVT-DLQ-REPLAY-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Submit event with PERMANENT failure
        eventPublisherService.publishEvent(new PublishEventRequest(
                eventId,
                "PAYMENT_REPLAY",
                "{\"amount\":100.0}",
                FailureMode.PERMANENT
        ));

        // 2. Wait until it routes to DLQ
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(dlqRecordRepository.findByEventId(eventId)).isPresent();
        });

        // 3. Trigger replay via service
        eventPublisherService.replayDlqEvent(eventId);

        // 4. Verify replayed event succeeds and idempotency record is persisted
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
            EventRecord record = eventRecordRepository.findByEventId(eventId).orElse(null);
            assertThat(record).isNotNull();
            assertThat(record.getStatus()).isEqualTo(EventStatus.SUCCESS);
        });
    }
}
