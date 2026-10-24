package com.college.lab.reliability.service;

import com.college.lab.reliability.entity.EventLog;
import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.repository.EventLogRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final EventRecordRepository eventRecordRepository;

    public EventLogService(EventLogRepository eventLogRepository, EventRecordRepository eventRecordRepository) {
        this.eventLogRepository = eventLogRepository;
        this.eventRecordRepository = eventRecordRepository;
    }

    /**
     * Persists log in an independent transaction so that it commits
     * even if the calling transaction rolls back due to an exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(String eventId, int attemptNumber, String status, long backoffDelayMs, String message) {
        EventLog log = new EventLog(eventId, attemptNumber, status, backoffDelayMs, message);
        eventLogRepository.save(log);
    }

    /**
     * Updates EventRecord status and attempt count in an independent transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEventStatus(String eventId, EventStatus status, int attemptCount) {
        eventRecordRepository.findByEventId(eventId).ifPresent(record -> {
            record.setStatus(status);
            record.setAttemptCount(attemptCount);
            eventRecordRepository.save(record);
        });
    }
}
