package com.hehe.security_service.service;

import com.hehe.security_service.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SecurityRetentionService {
    private final SecurityEventRepository repository;

    @Value("${security.raw-event-retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void deleteExpiredRawEvents() {
        repository.deleteByOccurredAtBefore(
                Instant.now().minus(Duration.ofDays(Math.max(1, retentionDays))));
    }
}
