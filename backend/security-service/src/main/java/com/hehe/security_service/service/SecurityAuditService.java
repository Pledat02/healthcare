package com.hehe.security_service.service;

import com.hehe.security_service.entity.SecurityAudit;
import com.hehe.security_service.repository.SecurityAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {
    private final SecurityAuditRepository repository;

    public void record(String actorId, String action, String targetType,
                       String targetValue, String reason, String traceId) {
        repository.save(SecurityAudit.builder()
                .id(UUID.randomUUID().toString())
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetValue(targetValue)
                .reason(reason)
                .occurredAt(Instant.now())
                .traceId(traceId)
                .build());
    }
}
