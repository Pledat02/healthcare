package com.hehe.appointment_service.client;

import com.hehe.appointment_service.dto.response.ApiResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
import com.hehe.appointment_service.dto.response.DoctorLeaveDto;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DoctorClient {

    private final RestClient doctorRestClient;

    public DoctorDto getDoctor(String doctorId) {
        ApiResponse<DoctorDto> res = doctorRestClient.get()
                .uri("/api/doctors/{id}", doctorId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, resp) -> {
                    throw new AppException(ErrorCode.DOCTOR_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<ApiResponse<DoctorDto>>() {});

        return res.getData();
    }
    public DoctorDto getMe() {
        ApiResponse<DoctorDto> res = doctorRestClient.get()
                .uri("/api/doctors/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, resp) -> {
                    throw new AppException(ErrorCode.DOCTOR_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<ApiResponse<DoctorDto>>() {});

        return res.getData();
    }

    // Ngay nghi sap toi cua bac si -> chan dat/doi lich trung ngay nghi
    public List<LocalDate> getLeaveDates(String doctorId) {
        ApiResponse<List<DoctorLeaveDto>> res = doctorRestClient.get()
                .uri("/api/doctors/{id}/leaves", doctorId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<DoctorLeaveDto>>>() {});

        if (res == null || res.getData() == null) return List.of();
        return res.getData().stream().map(DoctorLeaveDto::getLeaveDate).toList();
    }
}
