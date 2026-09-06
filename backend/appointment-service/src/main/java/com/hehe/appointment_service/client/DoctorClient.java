package com.hehe.appointment_service.client;

import com.hehe.appointment_service.config.ResilientCaller;
import com.hehe.appointment_service.dto.response.ApiResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
import com.hehe.appointment_service.dto.response.DoctorLeaveDto;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DoctorClient {

    // Ten circuit breaker + retry cho doctor-service (gom metric/trang thai theo downstream)
    private static final String CB = "doctor-service";

    private final RestClient doctorRestClient;
    private final InternalTokenClient internalTokenClient;
    private final ResilientCaller resilientCaller;   // BL-03: circuit breaker + retry

    // Cache ho so bac si theo id (doc nhieu, doi it). Cache HIT -> khoi goi doctor-service.
    // Chi cache khi thanh cong: neu throw (503/404) thi khong luu.
    @Cacheable(value = "doctor", key = "#doctorId")
    public DoctorDto getDoctor(String doctorId) {
        return resilientCaller.call(CB, ErrorCode.DOCTOR_SERVICE_UNAVAILABLE, () -> {
            ApiResponse<DoctorDto> res = doctorRestClient.get()
                    .uri("/api/doctors/{id}", doctorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, resp) -> {
                        throw new AppException(ErrorCode.DOCTOR_NOT_FOUND);
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<DoctorDto>>() {});
            return res.getData();
        });
    }

    public DoctorDto getMe() {
        return resilientCaller.call(CB, ErrorCode.DOCTOR_SERVICE_UNAVAILABLE, () -> {
            ApiResponse<DoctorDto> res = doctorRestClient.get()
                    .uri("/api/doctors/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())   // ← forward thẻ
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, resp) -> {
                        throw new AppException(ErrorCode.DOCTOR_NOT_FOUND);
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<DoctorDto>>() {});
            return res.getData();
        });
    }

    // Ngay nghi sap toi cua bac si -> chan dat/doi lich trung ngay nghi
    public List<LocalDate> getLeaveDates(String doctorId) {
        return resilientCaller.call(CB, ErrorCode.DOCTOR_SERVICE_UNAVAILABLE, () -> {
            ApiResponse<List<DoctorLeaveDto>> res = doctorRestClient.get()
                    .uri("/api/doctors/{id}/leaves", doctorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<DoctorLeaveDto>>>() {});

            if (res == null || res.getData() == null) return List.<LocalDate>of();
            return res.getData().stream().map(DoctorLeaveDto::getLeaveDate).toList();
        });
    }

    // Cong 1 luot danh gia vao bac si - goi endpoint noi bo (gated ADMIN) bang service-account.
    public void addRating(String doctorId, int stars) {
        resilientCaller.call(CB, ErrorCode.DOCTOR_SERVICE_UNAVAILABLE, () -> {
            doctorRestClient.post()
                    .uri("/api/doctors/{id}/ratings", doctorId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalTokenClient.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("stars", stars))
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    // Lay nhieu bac si trong 1 request (batch) -> lam giau lich hen khoi goi lap
    public List<DoctorDto> getDoctors(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return resilientCaller.call(CB, ErrorCode.DOCTOR_SERVICE_UNAVAILABLE, () -> {
            ApiResponse<List<DoctorDto>> res = doctorRestClient.get()
                    .uri(b -> b.path("/api/doctors/batch")
                            .queryParam("ids", String.join(",", ids)).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<DoctorDto>>>() {});
            return (res != null && res.getData() != null) ? res.getData() : List.<DoctorDto>of();
        });
    }
}
