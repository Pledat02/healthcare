package com.hehe.security_service.service;

import com.hehe.security_service.dto.UpdateAlertRequest;
import com.hehe.security_service.entity.SecurityAlert;
import com.hehe.security_service.repository.SecurityAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityAlertService {
    private final SecurityAlertRepository repository;
    private final SecurityAuditService auditService;

    @Transactional
    public SecurityAlert update(String id, UpdateAlertRequest request, String actorId, String traceId) {
        SecurityAlert alert = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo"));
        alert.setStatus(request.status());
        alert.setResolution(request.resolution() == null ? null : request.resolution().trim());
        alert.setAssignedTo(actorId);
        repository.save(alert);
        auditService.record(actorId, "UPDATE_ALERT", "ALERT", id,
                "status=" + request.status(), traceId);
        return alert;
    }
}
