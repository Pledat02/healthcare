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

import java.util.List;

@Component
@RequiredArgsConstructor
public class PatientClient {
    private final RestClient patientRestClient;
    private final InternalTokenClient internalTokenClient;
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

    // Lay ho so benh nhan theo id (dung khi gui mail cho lich do bac si xac nhan/hoan thanh)
    public PatientDto getPatient(String patientId) {
        ApiResponse<PatientDto> res = patientRestClient.get()
                .uri("/api/patients/{id}", patientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, resp) -> {
                    throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<ApiResponse<PatientDto>>() {});

        return res.getData();
    }

    /**
     * Lay nhieu benh nhan theo id trong 1 request (batch) bang token SERVICE-ACCOUNT.
     * Dung de appointment-service tu lam giau ten benh nhan cho lich hen cua bac si
     * -> bac si khong con goi thang /patients/batch (BR-06: khong do id tuy y).
     */
    public List<PatientDto> getPatients(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String csv = String.join(",", ids.stream().distinct().toList());
        ApiResponse<List<PatientDto>> res = patientRestClient.get()
                .uri(b -> b.path("/api/patients/batch").queryParam("ids", csv).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalTokenClient.token())
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<PatientDto>>>() {});
        return (res != null && res.getData() != null) ? res.getData() : List.of();
    }
}
