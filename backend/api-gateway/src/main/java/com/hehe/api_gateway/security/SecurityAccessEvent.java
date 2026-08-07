package com.hehe.api_gateway.security;

import java.time.Instant;

public record SecurityAccessEvent(
        String id,
        Instant occurredAt,
        String clientIp,
        String method,
        String normalizedPath,
        int status,
        long latencyMs,
        String userAgent,
        String eventType,
        int riskScore,
        String action,
        String traceId,
        String cloudflareRayId
) {
}
