package com.college.lab.reliability.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.net.Socket;

/**
 * Auto-detects whether an external Kafka broker is reachable on port 9092.
 * If no broker is detected (e.g. Docker is not running), this auto-starts
 * an embedded Kafka KRaft broker on port 9092 so the application runs seamlessly.
 */
@Configuration
public class EmbeddedKafkaAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedKafkaAutoConfig.class);
    private static EmbeddedKafkaBroker embeddedBroker;

    public EmbeddedKafkaAutoConfig(
            @Value("${app.kafka.embedded.enabled:true}") boolean embeddedEnabled,
            @Value("${app.kafka.topics.events:events-topic}") String eventsTopic,
            @Value("${app.kafka.topics.dlq:events-dlq}") String dlqTopic) {

        if (!embeddedEnabled) {
            log.info("[KAFKA-CONFIG] Embedded Kafka broker is explicitly disabled.");
            return;
        }

        if (isPortInUse("localhost", 9092)) {
            log.info("[KAFKA-CONFIG] Active Kafka broker detected on localhost:9092. Using external broker.");
            return;
        }

        log.info("[KAFKA-CONFIG] No Kafka broker detected on localhost:9092. Bootstrapping embedded Kafka KRaft broker on port 9092...");
        try {
            EmbeddedKafkaKraftBroker kraftBroker = new EmbeddedKafkaKraftBroker(1, 1, eventsTopic, dlqTopic);
            kraftBroker.kafkaPorts(9092);
            kraftBroker.afterPropertiesSet();
            embeddedBroker = kraftBroker;
            log.info("[KAFKA-CONFIG] Embedded Kafka KRaft broker successfully started on port 9092 with topics: '{}', '{}'.",
                    eventsTopic, dlqTopic);
        } catch (Exception e) {
            log.warn("[KAFKA-CONFIG] Could not start embedded Kafka broker: {}", e.getMessage());
        }
    }

    private boolean isPortInUse(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void stop() {
        if (embeddedBroker != null) {
            log.info("[KAFKA-CONFIG] Shutting down embedded Kafka KRaft broker...");
            try {
                embeddedBroker.destroy();
            } catch (Exception ignored) {
            }
        }
    }
}
