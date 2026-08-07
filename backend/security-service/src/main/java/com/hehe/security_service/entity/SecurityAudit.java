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
@Table(name = "security_audits", indexes = {
        @Index(name = "idx_security_audit_time", columnList = "occurred_at"),
        @Index(name = "idx_security_audit_actor", columnList = "actor_id,occurred_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAudit {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_value", nullable = false, length = 256)
    private String targetValue;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "trace_id", length = 64)
    private String traceId;
}
