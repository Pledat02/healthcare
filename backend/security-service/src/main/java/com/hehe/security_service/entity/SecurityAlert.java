package com.hehe.security_service.entity;

import com.hehe.security_service.model.AlertSeverity;
import com.hehe.security_service.model.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "security_alerts", indexes = {
        @Index(name = "idx_security_alert_status_time", columnList = "status,last_seen_at"),
        @Index(name = "idx_security_alert_ip_time", columnList = "client_ip,last_seen_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlert {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 64)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AlertStatus status;

    @Column(name = "client_ip", nullable = false, length = 64)
    private String clientIp;

    @Column(name = "normalized_route", nullable = false, length = 512)
    private String normalizedRoute;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "event_count", nullable = false)
    private long eventCount;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "assigned_to", length = 128)
    private String assignedTo;

    @Column(length = 1000)
    private String resolution;
}
