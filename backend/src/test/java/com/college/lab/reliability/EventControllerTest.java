package com.college.lab.reliability;

import com.college.lab.reliability.controller.EventController;
import com.college.lab.reliability.dto.DashboardStatsResponse;
import com.college.lab.reliability.dto.PublishEventRequest;
import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.entity.FailureMode;
import com.college.lab.reliability.repository.DlqRecordRepository;
import com.college.lab.reliability.repository.EventLogRepository;
import com.college.lab.reliability.repository.EventRecordRepository;
import com.college.lab.reliability.service.DashboardService;
import com.college.lab.reliability.service.EventPublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventPublisherService eventPublisherService;

    @MockBean
    private EventRecordRepository eventRecordRepository;

    @MockBean
    private EventLogRepository eventLogRepository;

    @MockBean
    private DlqRecordRepository dlqRecordRepository;

    @MockBean
    private DashboardService dashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/events creates and publishes event successfully")
    void testPublishEventEndpoint() throws Exception {
        PublishEventRequest request = new PublishEventRequest(
                "EVT-CTRL-1",
                "USER_REGISTERED",
                "{\"email\":\"student@college.edu\"}",
                FailureMode.NONE
        );

        EventRecord record = new EventRecord(
                request.getEventId(),
                request.getEventType(),
                request.getPayload(),
                request.getFailureMode(),
                EventStatus.SUBMITTED
        );

        when(eventPublisherService.publishEvent(any(PublishEventRequest.class))).thenReturn(record);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value("EVT-CTRL-1"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats returns real counters")
    void testGetDashboardStatsEndpoint() throws Exception {
        when(dashboardService.getStats()).thenReturn(new DashboardStatsResponse(10, 7, 1, 1, 1, 1));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSubmitted").value(10))
                .andExpect(jsonPath("$.data.successfullyProcessed").value(7))
                .andExpect(jsonPath("$.data.dlqEvents").value(1));
    }

    @Test
    @DisplayName("GET /api/events/{eventId} returns specific event status")
    void testGetEventStatusEndpoint() throws Exception {
        EventRecord record = new EventRecord(
                "EVT-CTRL-2",
                "ORDER_PAID",
                "{\"amount\":99.9}",
                FailureMode.NONE,
                EventStatus.SUCCESS
        );

        when(eventRecordRepository.findByEventId("EVT-CTRL-2")).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/events/EVT-CTRL-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value("EVT-CTRL-2"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/events/dlq returns DLQ records")
    void testGetDlqEndpoint() throws Exception {
        when(dlqRecordRepository.findAllByOrderByReceivedAtDesc()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/events/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/events/dlq/{eventId}/replay triggers controlled event replay")
    void testReplayDlqEndpoint() throws Exception {
        EventRecord record = new EventRecord(
                "EVT-REPLAY-1",
                "ORDER_PAID",
                "{\"amount\":150.0}",
                FailureMode.NONE,
                EventStatus.SUBMITTED
        );

        when(eventPublisherService.replayDlqEvent("EVT-REPLAY-1")).thenReturn(record);

        mockMvc.perform(post("/api/events/dlq/EVT-REPLAY-1/replay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value("EVT-REPLAY-1"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }
}
