package com.college.lab.reliability.dto;

import com.college.lab.reliability.entity.DlqRecord;
import java.time.LocalDateTime;

public class DlqRecordResponse {
    private Long id;
    private String eventId;
    private String originalTopic;
    private Integer originalPartition;
    private Long originalOffset;
    private String payload;
    private String failureReason;
    private String headers;
    private int attemptsMade;
    private LocalDateTime receivedAt;

    public DlqRecordResponse() {
    }

    public static DlqRecordResponse fromEntity(DlqRecord dlq) {
        DlqRecordResponse dto = new DlqRecordResponse();
        dto.setId(dlq.getId());
        dto.setEventId(dlq.getEventId());
        dto.setOriginalTopic(dlq.getOriginalTopic());
        dto.setOriginalPartition(dlq.getOriginalPartition());
        dto.setOriginalOffset(dlq.getOriginalOffset());
        dto.setPayload(dlq.getPayload());
        dto.setFailureReason(dlq.getFailureReason());
        dto.setHeaders(dlq.getHeaders());
        dto.setAttemptsMade(dlq.getAttemptsMade());
        dto.setReceivedAt(dlq.getReceivedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public void setOriginalTopic(String originalTopic) {
        this.originalTopic = originalTopic;
    }

    public Integer getOriginalPartition() {
        return originalPartition;
    }

    public void setOriginalPartition(Integer originalPartition) {
        this.originalPartition = originalPartition;
    }

    public Long getOriginalOffset() {
        return originalOffset;
    }

    public void setOriginalOffset(Long originalOffset) {
        this.originalOffset = originalOffset;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public int getAttemptsMade() { return attemptsMade; }

    public void setAttemptsMade(int attemptsMade) { this.attemptsMade = attemptsMade; }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
