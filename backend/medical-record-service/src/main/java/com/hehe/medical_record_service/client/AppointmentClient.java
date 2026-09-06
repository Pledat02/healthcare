package com.hehe.medical_record_service.client;

import com.hehe.medical_record_service.config.ResilientCaller;
import com.hehe.medical_record_service.dto.response.ApiResponse;
import com.hehe.medical_record_service.dto.response.AppointmentDto;
import com.hehe.medical_record_service.exception.AppException;
import com.hehe.medical_record_service.exception.ErrorCode;
import com.hehe.medical_record_service.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AppointmentClient {

    private static final String CB = "appointment-service";

    private final RestClient appointmentRestClient;
    private final ResilientCaller resilientCaller;   // BL-03: circuit breaker + retry

    public AppointmentDto getAppointment(String appointmentId) {
        return resilientCaller.call(CB, ErrorCode.SERVICE_UNAVAILABLE, () -> {
            ApiResponse<AppointmentDto> res = appointmentRestClient.get()
                    .uri("/api/appointments/{id}", appointmentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, resp) -> {
                        throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<AppointmentDto>>() {});
            return res.getData();
        });
    }
}
