package com.hehe.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class SecurityEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(SecurityEventPublisher.class);
    private final KafkaTemplate<String, Object> securityKafkaTemplate;
    private final AtomicLong lastFailureLogAt = new AtomicLong(0);

    public SecurityEventPublisher(KafkaTemplate<String, Object> securityKafkaTemplate) {
        this.securityKafkaTemplate = securityKafkaTemplate;
    }

    @Value("${security.event-topic:security-access-events}")
    private String topic;

    public void publish(SecurityAccessEvent event) {
        try {
            securityKafkaTemplate.send(topic, event.clientIp(), event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logFailure("Khong the gui security event " + event.id(), error);
                        }
                    });
        } catch (RuntimeException exception) {
            // Logging khong duoc lam hong request nghiep vu; van phai phat canh bao ro rang.
            logFailure("Kafka security event unavailable", exception);
        }
    }

    private void logFailure(String message, Throwable error) {
        long now = System.currentTimeMillis();
        long previous = lastFailureLogAt.get();
        if (now - previous >= 30_000 && lastFailureLogAt.compareAndSet(previous, now)) {
            log.error("{}: {}", message, error.getMessage());
        }
    }
}
