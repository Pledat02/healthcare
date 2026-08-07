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
@Table(name = "security_ip_blocks", indexes = {
        @Index(name = "idx_ip_block_active", columnList = "active,expires_at"),
        @Index(name = "idx_ip_block_ip", columnList = "ip_address")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpBlock {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean automatic;

    @Column(name = "blocked_by", nullable = false, length = 128)
    private String blockedBy;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "unblocked_by", length = 128)
    private String unblockedBy;

    @Column(name = "unblocked_at")
    private Instant unblockedAt;
}
