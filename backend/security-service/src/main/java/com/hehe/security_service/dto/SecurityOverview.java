package com.hehe.security_service.dto;

public record SecurityOverview(
        long eventsLast24Hours,
        long highRiskEventsLast24Hours,
        long openAlerts,
        long activeBlocks
) {
}
