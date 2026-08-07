package com.hehe.security_service.controller;

import com.hehe.security_service.dto.ApiResponse;
import com.hehe.security_service.dto.CreateIpBlockRequest;
import com.hehe.security_service.dto.SecurityOverview;
import com.hehe.security_service.dto.UpdateAlertRequest;
import com.hehe.security_service.entity.IpBlock;
import com.hehe.security_service.entity.SecurityAlert;
import com.hehe.security_service.entity.SecurityAudit;
import com.hehe.security_service.entity.SecurityEvent;
import com.hehe.security_service.model.AlertStatus;
import com.hehe.security_service.repository.SecurityAuditRepository;
import com.hehe.security_service.service.IpBlockService;
import com.hehe.security_service.service.SecurityAlertService;
import com.hehe.security_service.service.SecurityQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {
    private final SecurityQueryService queryService;
    private final SecurityAlertService alertService;
    private final IpBlockService blockService;
    private final SecurityAuditRepository auditRepository;

    @GetMapping("/overview")
    ApiResponse<SecurityOverview> overview() {
        return ApiResponse.ok(queryService.overview());
    }

    @GetMapping("/events")
    ApiResponse<Page<SecurityEvent>> events(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(queryService.events(page, size, ip, type));
    }

    @GetMapping("/alerts")
    ApiResponse<Page<SecurityAlert>> alerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) AlertStatus status) {
        return ApiResponse.ok(queryService.alerts(page, size, status));
    }

    @PatchMapping("/alerts/{id}")
    ApiResponse<SecurityAlert> updateAlert(
            @PathVariable String id,
            @Valid @RequestBody UpdateAlertRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(alertService.update(id, request, actor(jwt), traceId()));
    }

    @GetMapping("/blocks")
    ApiResponse<List<IpBlock>> blocks() {
        return ApiResponse.ok(blockService.list());
    }

    @PostMapping("/blocks")
    ApiResponse<IpBlock> block(
            @Valid @RequestBody CreateIpBlockRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.created(blockService.block(request, actor(jwt), traceId()));
    }

    @DeleteMapping("/blocks/{id}")
    ApiResponse<IpBlock> unblock(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(blockService.unblock(id, actor(jwt), traceId()));
    }

    @GetMapping("/audits")
    ApiResponse<Page<SecurityAudit>> audits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int boundedSize = Math.max(1, Math.min(size, 100));
        return ApiResponse.ok(auditRepository.findAllByOrderByOccurredAtDesc(
                PageRequest.of(Math.max(0, page), boundedSize)));
    }

    private String actor(Jwt jwt) {
        return jwt == null ? "unknown" : jwt.getSubject();
    }

    private String traceId() {
        return MDC.get("traceId");
    }
}
