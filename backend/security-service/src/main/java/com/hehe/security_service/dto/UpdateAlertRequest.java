package com.hehe.security_service.dto;

import com.hehe.security_service.model.AlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAlertRequest(
        @NotNull AlertStatus status,
        @Size(max = 1000) String resolution
) {
}
