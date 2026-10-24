package com.college.lab.reliability.service;

import com.college.lab.reliability.entity.FailureMode;
import com.college.lab.reliability.exception.PermanentProcessingException;
import com.college.lab.reliability.exception.TransientProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class FailureSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(FailureSimulatorService.class);

    private final int transientFailCount;

    public FailureSimulatorService(
            @Value("${app.kafka.simulator.transient-fail-count:2}") int transientFailCount) {
        this.transientFailCount = transientFailCount;
    }

    /**
     * Evaluates whether the event should fail based on its FailureMode.
     * Throws TransientProcessingException for transient failures,
     * throws PermanentProcessingException for permanent failures,
     * or proceeds normally for NONE or when transient retries threshold is met.
     */
    public void evaluateSimulation(String eventId, FailureMode failureMode, int attemptNumber) {
        if (failureMode == null || failureMode == FailureMode.NONE) {
            log.info("[Simulator] Event '{}' has mode NONE. Proceeding with successful execution.", eventId);
            return;
        }

        if (failureMode == FailureMode.TRANSIENT) {
            if (attemptNumber <= transientFailCount) {
                log.warn("[Simulator] Simulating TRANSIENT failure for event '{}' on attempt {}/{} (will retry).",
                        eventId, attemptNumber, transientFailCount + 1);
                throw new TransientProcessingException(
                        "Simulated transient network/database timeout on attempt " + attemptNumber +
                        " (Configured to succeed on attempt " + (transientFailCount + 1) + ")");
            } else {
                log.info("[Simulator] Transient failure threshold ({}) reached for event '{}'. Attempt {} will SUCCEED.",
                        transientFailCount, eventId, attemptNumber);
            }
        } else if (failureMode == FailureMode.PERMANENT) {
            log.error("[Simulator] Simulating PERMANENT unrecoverable error for event '{}' on attempt {}.",
                    eventId, attemptNumber);
            throw new PermanentProcessingException(
                    "Simulated unrecoverable business violation / bad payload on attempt " + attemptNumber +
                    " (Destined for Dead Letter Queue)");
        }
    }
}
