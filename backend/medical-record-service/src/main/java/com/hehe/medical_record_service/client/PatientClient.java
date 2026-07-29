package com.hehe.medical_record_service.client;

import com.hehe.medical_record_service.dto.response.ApiResponse;
import com.hehe.medical_record_service.dto.response.PatientDto;
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
public class PatientClient {
    private final RestClient patientRestClient;
    public PatientDto getMe() {
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
    public PatientDto getPatient(String id) {
        ApiResponse<PatientDto> res = patientRestClient.get()
                .uri("/api/patients/{id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, resp) -> {
                    throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<ApiResponse<PatientDto>>() {});

        return res.getData();
    }
}
