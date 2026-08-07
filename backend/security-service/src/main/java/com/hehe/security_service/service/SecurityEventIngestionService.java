package com.hehe.security_service.service;

import com.hehe.security_service.dto.SecurityAccessEvent;
import com.hehe.security_service.entity.SecurityAlert;
import com.hehe.security_service.entity.SecurityEvent;
import com.hehe.security_service.model.AlertSeverity;
import com.hehe.security_service.model.AlertStatus;
import com.hehe.security_service.repository.SecurityAlertRepository;
import com.hehe.security_service.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityEventIngestionService {
    private static final int ALERT_THRESHOLD = 25;
    private static final List<AlertStatus> OPEN_STATUSES =
            List.of(AlertStatus.NEW, AlertStatus.INVESTIGATING, AlertStatus.CONFIRMED);

    private final SecurityEventRepository eventRepository;
    private final SecurityAlertRepository alertRepository;

    @KafkaListener(topics = "${security.event-topic}")
    @Transactional
    public void consume(SecurityAccessEvent incoming) {
        if (incoming == null || incoming.id() == null || eventRepository.existsById(incoming.id())) {
            return;
        }
        SecurityEvent event = SecurityEvent.builder()
                .id(incoming.id())
                .occurredAt(incoming.occurredAt() == null ? Instant.now() : incoming.occurredAt())
                .clientIp(limit(incoming.clientIp(), 64, "unknown"))
                .method(limit(incoming.method(), 12, "UNKNOWN"))
                .normalizedPath(limit(incoming.normalizedPath(), 512, "/"))
                .status(incoming.status())
                .latencyMs(Math.max(0, incoming.latencyMs()))
                .userAgent(limit(incoming.userAgent(), 512, null))
                .eventType(limit(incoming.eventType(), 64, "ACCESS"))
                .riskScore(Math.max(0, Math.min(100, incoming.riskScore())))
                .action(limit(incoming.action(), 32, "ALLOWED"))
                .traceId(limit(incoming.traceId(), 64, null))
                .cloudflareRayId(limit(incoming.cloudflareRayId(), 64, null))
                .build();
        eventRepository.save(event);
        if (event.getRiskScore() >= ALERT_THRESHOLD) {
            upsertAlert(event);
        }
    }

    private void upsertAlert(SecurityEvent event) {
        Instant cutoff = event.getOccurredAt().minus(Duration.ofMinutes(10));
        SecurityAlert alert = alertRepository
                .findFirstByClientIpAndTypeAndStatusInAndLastSeenAtAfterOrderByLastSeenAtDesc(
                        event.getClientIp(), event.getEventType(), OPEN_STATUSES, cutoff)
                .orElseGet(() -> SecurityAlert.builder()
                        .id(UUID.randomUUID().toString())
                        .type(event.getEventType())
                        .severity(severity(event.getRiskScore()))
                        .status(AlertStatus.NEW)
                        .clientIp(event.getClientIp())
                        .normalizedRoute(event.getNormalizedPath())
                        .riskScore(event.getRiskScore())
                        .eventCount(0)
                        .firstSeenAt(event.getOccurredAt())
                        .lastSeenAt(event.getOccurredAt())
                        .build());
        alert.setEventCount(alert.getEventCount() + 1);
        alert.setLastSeenAt(event.getOccurredAt());
        alert.setRiskScore(Math.max(alert.getRiskScore(), event.getRiskScore()));
        alert.setSeverity(severity(alert.getRiskScore()));
        alertRepository.save(alert);
    }

    private AlertSeverity severity(int risk) {
        if (risk >= 90) return AlertSeverity.CRITICAL;
        if (risk >= 70) return AlertSeverity.HIGH;
        if (risk >= 40) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }

    private String limit(String value, int max, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String sanitized = value.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= max ? sanitized : sanitized.substring(0, max);
    }
}
