package com.hehe.security_service.service;

import com.hehe.security_service.dto.SecurityAccessEvent;
import com.hehe.security_service.entity.SecurityAlert;
import com.hehe.security_service.repository.SecurityAlertRepository;
import com.hehe.security_service.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityEventIngestionServiceTest {
    private final SecurityEventRepository eventRepository = mock(SecurityEventRepository.class);
    private final SecurityAlertRepository alertRepository = mock(SecurityAlertRepository.class);
    private final SecurityEventIngestionService service =
            new SecurityEventIngestionService(eventRepository, alertRepository);

    @Test
    void highRiskEventCreatesAlert() {
        Instant now = Instant.now();
        SecurityAccessEvent event = new SecurityAccessEvent(
                "evt-1", now, "203.0.113.10", "GET", "/.env",
                404, 12, "scanner", "SUSPICIOUS_PATH", 90, "ALLOWED", null, "abc123-SIN");
        when(eventRepository.existsById("evt-1")).thenReturn(false);
        when(alertRepository.findFirstByClientIpAndTypeAndStatusInAndLastSeenAtAfterOrderByLastSeenAtDesc(
                eq("203.0.113.10"), eq("SUSPICIOUS_PATH"), anyList(), any()))
                .thenReturn(Optional.empty());

        service.consume(event);

        ArgumentCaptor<SecurityAlert> alert = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(alertRepository).save(alert.capture());
        assertThat(alert.getValue().getSeverity().name()).isEqualTo("CRITICAL");
        assertThat(alert.getValue().getEventCount()).isEqualTo(1);
    }
}
