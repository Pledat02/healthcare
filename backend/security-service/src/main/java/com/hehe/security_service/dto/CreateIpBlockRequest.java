package com.hehe.security_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIpBlockRequest(
        @NotBlank @Size(max = 64) String ip,
        @Min(1) @Max(525600) Long durationMinutes,
        @NotBlank @Size(max = 500) String reason
) {
}
