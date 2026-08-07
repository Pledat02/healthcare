package com.hehe.security_service.service;

import com.hehe.security_service.dto.SecurityOverview;
import com.hehe.security_service.entity.SecurityAlert;
import com.hehe.security_service.entity.SecurityEvent;
import com.hehe.security_service.model.AlertStatus;
import com.hehe.security_service.repository.IpBlockRepository;
import com.hehe.security_service.repository.SecurityAlertRepository;
import com.hehe.security_service.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityQueryService {
    private final SecurityEventRepository eventRepository;
    private final SecurityAlertRepository alertRepository;
    private final IpBlockRepository blockRepository;

    public SecurityOverview overview() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        return new SecurityOverview(
                eventRepository.countByOccurredAtAfter(cutoff),
                eventRepository.countByRiskScoreGreaterThanEqualAndOccurredAtAfter(70, cutoff),
                alertRepository.countByStatusIn(List.of(AlertStatus.NEW, AlertStatus.INVESTIGATING, AlertStatus.CONFIRMED)),
                blockRepository.countByActiveTrue());
    }

    public Page<SecurityEvent> events(int page, int size, String ip, String type) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), boundedSize(size),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        boolean hasIp = ip != null && !ip.isBlank();
        boolean hasType = type != null && !type.isBlank();
        if (hasIp && hasType) {
            return eventRepository.findByClientIpContainingIgnoreCaseAndEventType(ip.trim(), type.trim(), pageable);
        }
        if (hasIp) return eventRepository.findByClientIpContainingIgnoreCase(ip.trim(), pageable);
        if (hasType) return eventRepository.findByEventType(type.trim(), pageable);
        return eventRepository.findAll(pageable);
    }

    public Page<SecurityAlert> alerts(int page, int size, AlertStatus status) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), boundedSize(size),
                Sort.by(Sort.Direction.DESC, "lastSeenAt"));
        return status == null ? alertRepository.findAll(pageable) : alertRepository.findByStatus(status, pageable);
    }

    private int boundedSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }
}
