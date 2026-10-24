package com.college.lab.reliability.service;

import com.college.lab.reliability.dto.EventMessage;
import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.entity.ProcessedEvent;
import com.college.lab.reliability.exception.TransientProcessingException;
import com.college.lab.reliability.repository.ProcessedEventRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final EventLogService eventLogService;
    private final FailureSimulatorService failureSimulatorService;
    private final EventRecordRepository eventRecordRepository;

    @Value("${app.kafka.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${app.kafka.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${app.kafka.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    public EventProcessingService(
            ProcessedEventRepository processedEventRepository,
            EventLogService eventLogService,
            FailureSimulatorService failureSimulatorService,
            EventRecordRepository eventRecordRepository) {
        this.processedEventRepository = processedEventRepository;
        this.eventLogService = eventLogService;
        this.failureSimulatorService = failureSimulatorService;
        this.eventRecordRepository = eventRecordRepository;
    }

    /**
     * Calculates the exponential backoff delay for the current attempt number.
     * Attempt 1 has 0ms delay.
     * Attempt 2 has initialIntervalMs (e.g. 1000ms).
     * Attempt 3 has initialIntervalMs * multiplier (e.g. 2000ms).
     * Attempt 4 has initialIntervalMs * multiplier^2 (e.g. 4000ms).
     */
    public long calculateBackoffDelay(int attempt) {
        if (attempt <= 1) {
            return 0L;
        }
        long delay = (long) (initialIntervalMs * Math.pow(multiplier, attempt - 2));
        return Math.min(delay, maxIntervalMs);
    }

    /**
     * Main business processing logic executed under transactional isolation.
     * Ensures atomic business state updates and database idempotency.
     */
    @Transactional
    public void processEvent(EventMessage message) {
        String eventId = message.getEventId();
        // The event row is the durable attempt counter. This survives backend restarts,
        // unlike an in-memory counter, and updateEventStatus commits before processing.
        int attemptNumber = eventRecordRepository.findByEventId(eventId)
                .map(record -> record.getAttemptCount() + 1)
                .orElse(1);
        long backoffDelay = calculateBackoffDelay(attemptNumber);

        log.info("[PROCESSING] Beginning attempt #{} for event '{}' (Backoff delay: {} ms)",
                attemptNumber, eventId, backoffDelay);

        // 1. Idempotency Check: Was this event already successfully processed?
        if (processedEventRepository.existsById(eventId)) {
            log.warn("[IDEMPOTENCY] Event '{}' has already been successfully processed. Skipping business operation.", eventId);
            eventLogService.recordLog(
                    eventId,
                    attemptNumber,
                    "DUPLICATE_SKIPPED",
                    backoffDelay,
                    "Idempotency guard: Event ID '" + eventId + "' already exists in processed_events. Duplicate execution prevented."
            );
            eventLogService.updateEventStatus(eventId, EventStatus.DUPLICATE_IGNORED, attemptNumber);
            return;
        }

        // 2. Log attempt commencement
        String currentStatus = attemptNumber > 1 ? "RETRYING" : "ATTEMPT_STARTED";
        eventLogService.updateEventStatus(
                eventId,
                attemptNumber > 1 ? EventStatus.RETRYING : EventStatus.PROCESSING,
                attemptNumber
        );
        eventLogService.recordLog(
                eventId,
                attemptNumber,
                currentStatus,
                backoffDelay,
                "Starting processing attempt #" + attemptNumber + " (Failure Mode: " + message.getFailureMode() + ")"
        );

        // 3. Evaluate deterministic failure simulation
        try {
            failureSimulatorService.evaluateSimulation(eventId, message.getFailureMode(), attemptNumber);
        } catch (RuntimeException simulationEx) {
            // Log failure details before throwing so log is retained when transaction rolls back
            boolean isTransient = simulationEx instanceof TransientProcessingException;
            String failStatus = isTransient ? "FAILED_TRANSIENT" : "FAILED_PERMANENT";

            eventLogService.recordLog(
                    eventId,
                    attemptNumber,
                    failStatus,
                    backoffDelay,
                    "Failure encountered on attempt #" + attemptNumber + ": " + simulationEx.getMessage()
            );
            eventLogService.updateEventStatus(
                    eventId,
                    isTransient ? EventStatus.RETRYING : EventStatus.FAILED,
                    attemptNumber
            );

            // Re-throw so Spring Kafka's DefaultErrorHandler activates exponential backoff or DLQ routing
            throw simulationEx;
        }

        // 4. Execute atomic business operation
        executeBusinessOperation(message);

        // 5. Persist idempotency record in the same transaction
        try {
            ProcessedEvent processedEvent = new ProcessedEvent(
                    eventId,
                    "Successfully processed payload: " + message.getEventType()
            );
            processedEventRepository.save(processedEvent);
        } catch (DataIntegrityViolationException ex) {
            log.warn("[CONCURRENCY] Concurrent duplicate execution caught by database constraint for event '{}'", eventId);
            eventLogService.recordLog(
                    eventId,
                    attemptNumber,
                    "DUPLICATE_SKIPPED",
                    backoffDelay,
                    "Concurrent duplicate blocked by database uniqueness constraint."
            );
            eventLogService.updateEventStatus(eventId, EventStatus.DUPLICATE_IGNORED, attemptNumber);
            return;
        }

        // 6. Update status to SUCCESS and record success log
        eventLogService.updateEventStatus(eventId, EventStatus.SUCCESS, attemptNumber);
        eventLogService.recordLog(
                eventId,
                attemptNumber,
                "SUCCESS",
                backoffDelay,
                "Event successfully processed on attempt #" + attemptNumber + ". Idempotency record saved."
        );
        log.info("[COMPLETED] Event '{}' processed successfully on attempt #{}", eventId, attemptNumber);
    }

    private void executeBusinessOperation(EventMessage message) {
        // Business operation simulation (e.g. updating ledger, reserving inventory)
        log.info("[BUSINESS-LOGIC] Executing core business logic for event type '{}', payload: {}",
                message.getEventType(), message.getPayload());
    }
}
