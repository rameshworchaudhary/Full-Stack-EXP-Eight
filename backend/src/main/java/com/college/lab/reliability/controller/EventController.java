package com.college.lab.reliability.controller;

import com.college.lab.reliability.dto.*;
import com.college.lab.reliability.entity.DlqRecord;
import com.college.lab.reliability.entity.EventLog;
import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.exception.ResourceNotFoundException;
import com.college.lab.reliability.repository.DlqRecordRepository;
import com.college.lab.reliability.repository.EventLogRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import com.college.lab.reliability.service.DashboardService;
import com.college.lab.reliability.service.EventPublisherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
public class EventController {

    private final EventPublisherService eventPublisherService;
    private final EventRecordRepository eventRecordRepository;
    private final EventLogRepository eventLogRepository;
    private final DlqRecordRepository dlqRecordRepository;
    private final DashboardService dashboardService;

    public EventController(
            EventPublisherService eventPublisherService,
            EventRecordRepository eventRecordRepository,
            EventLogRepository eventLogRepository,
            DlqRecordRepository dlqRecordRepository,
            DashboardService dashboardService) {
        this.eventPublisherService = eventPublisherService;
        this.eventRecordRepository = eventRecordRepository;
        this.eventLogRepository = eventLogRepository;
        this.dlqRecordRepository = dlqRecordRepository;
        this.dashboardService = dashboardService;
    }

    /**
     * POST /api/events — publish an event to events-topic.
     */
    @PostMapping("/events")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(
            @Valid @RequestBody PublishEventRequest request) {
        EventRecord record = eventPublisherService.publishEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Event submitted successfully to Kafka", EventResponse.fromEntity(record)));
    }

    /**
     * GET /api/events — retrieve event history from persisted application records.
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvents() {
        List<EventResponse> events = eventRecordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Event history retrieved", events));
    }

    /**
     * GET /api/events/dlq — inspect persisted DLQ records.
     * Note: placed before /{eventId} to prevent path variable shadowing.
     */
    @GetMapping("/events/dlq")
    public ResponseEntity<ApiResponse<List<DlqRecordResponse>>> getDlqRecords() {
        List<DlqRecordResponse> dlqRecords = dlqRecordRepository.findAllByOrderByReceivedAtDesc()
                .stream()
                .map(DlqRecordResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("DLQ records retrieved", dlqRecords));
    }

    /**
     * POST /api/events/dlq/{eventId}/replay — replay a Dead Letter Queue event in a controlled, idempotent manner.
     */
    @PostMapping("/events/dlq/{eventId}/replay")
    public ResponseEntity<ApiResponse<EventResponse>> replayDlqEvent(@PathVariable String eventId) {
        EventRecord record = eventPublisherService.replayDlqEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok("DLQ event re-queued for processing", EventResponse.fromEntity(record)));
    }

    /**
     * GET /api/events/{eventId} — retrieve processing status.
     */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventStatus(@PathVariable String eventId) {
        EventRecord record = eventRecordRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with eventId: " + eventId));
        return ResponseEntity.ok(ApiResponse.ok("Event status retrieved", EventResponse.fromEntity(record)));
    }

    /**
     * GET /api/dashboard/stats — retrieve real counters.
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getStats();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard statistics retrieved", stats));
    }

    /**
     * GET /api/events/{eventId}/logs — retrieve processing attempts and logs.
     */
    @GetMapping("/events/{eventId}/logs")
    public ResponseEntity<ApiResponse<List<EventLogResponse>>> getEventLogs(@PathVariable String eventId) {
        List<EventLogResponse> logs = eventLogRepository.findByEventIdOrderByAttemptNumberAsc(eventId)
                .stream()
                .map(EventLogResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Event logs retrieved", logs));
    }
}
