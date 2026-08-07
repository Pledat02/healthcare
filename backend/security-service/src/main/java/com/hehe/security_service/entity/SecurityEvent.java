package com.hehe.security_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_security_event_time", columnList = "occurred_at"),
        @Index(name = "idx_security_event_ip_time", columnList = "client_ip,occurred_at"),
        @Index(name = "idx_security_event_type_time", columnList = "event_type,occurred_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "client_ip", nullable = false, length = 64)
    private String clientIp;

    @Column(nullable = false, length = 12)
    private String method;

    @Column(name = "normalized_path", nullable = false, length = 512)
    private String normalizedPath;

    @Column(nullable = false)
    private int status;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "cloudflare_ray_id", length = 64)
    private String cloudflareRayId;
}
