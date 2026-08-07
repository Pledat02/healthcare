package com.hehe.security_service.repository;

import com.hehe.security_service.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, String> {
    Page<SecurityEvent> findByClientIpContainingIgnoreCase(String clientIp, Pageable pageable);
    Page<SecurityEvent> findByEventType(String eventType, Pageable pageable);
    Page<SecurityEvent> findByClientIpContainingIgnoreCaseAndEventType(String clientIp, String eventType, Pageable pageable);
    long countByOccurredAtAfter(Instant cutoff);
    long countByRiskScoreGreaterThanEqualAndOccurredAtAfter(int riskScore, Instant cutoff);
    long deleteByOccurredAtBefore(Instant cutoff);
}
