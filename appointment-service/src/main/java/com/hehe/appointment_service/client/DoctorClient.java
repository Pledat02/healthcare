package com.hehe.appointment_service.client;

import com.hehe.appointment_service.dto.response.ApiResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
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


}
