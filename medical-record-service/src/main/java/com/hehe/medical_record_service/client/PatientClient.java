package com.hehe.appointment_service.client;

import com.hehe.appointment_service.dto.response.ApiResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
import com.hehe.appointment_service.dto.response.PatientDto;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
@RequiredArgsConstructor
public class PatientClient {
    private final RestClient patientRestClient;
    public PatientDto getPatient() {
        ApiResponse<PatientDto> res = patientRestClient.get()
                .uri("/api/patients/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, resp) -> {
                    throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<ApiResponse<PatientDto>>() {});

        return res.getData();
    }
}
