package com.hehe.appointment_service.client;

import com.hehe.appointment_service.dto.response.ApiResponse;
import com.hehe.appointment_service.dto.response.PatientDto;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.utils.SecurityUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * BL-03 (ban annotation): bao moi lenh goi patient-service bang @CircuitBreaker + @Retry.
 *
 * Cau hinh nam trong application.yaml duoi prefix `resilience4j.*` (instance ten "patient-service"):
 *  - retry: chi retry loi ha nguon (timeout/IO, 5xx); bo qua AppException (loi nghiep vu 404).
 *  - circuitbreaker: cung bo qua AppException khi tinh ti le loi.
 *  - thu tu aspect: circuit-breaker-aspect-order > retry-aspect-order -> CB boc NGOAI retry
 *    (circuit MO thi bao loi ngay, khong retry) - giu dung nguyen tac cu cua ResilientCaller.
 *
 * fallbackMethod chay khi method nem BAT KY exception nao:
 *  - Neu la AppException (vd 404) -> nem lai nguyen ven (giu dung ma loi nghiep vu).
 *  - Con lai (timeout/5xx/circuit MO) -> AppException 503 PATIENT_SERVICE_UNAVAILABLE.
 */
@Component
@RequiredArgsConstructor
public class PatientClient {

    private static final String CB = "patient-service";

    private final RestClient patientRestClient;
    private final InternalTokenClient internalTokenClient;

    @CircuitBreaker(name = CB, fallbackMethod = "getPatientFallback")
    @Retry(name = CB)
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

    @SuppressWarnings("unused")
    private PatientDto getPatientFallback(Throwable t) {
        return unavailable(t);
    }

    // Lay ho so benh nhan theo id (dung khi gui mail cho lich do bac si xac nhan/hoan thanh)
    @CircuitBreaker(name = CB, fallbackMethod = "getPatientByIdFallback")
    @Retry(name = CB)
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

    @SuppressWarnings("unused")
    private PatientDto getPatientByIdFallback(String patientId, Throwable t) {
        return unavailable(t);
    }

    /**
     * Lay nhieu benh nhan theo id trong 1 request (batch) bang token SERVICE-ACCOUNT.
     * Dung de appointment-service tu lam giau ten benh nhan cho lich hen cua bac si
     * -> bac si khong con goi thang /patients/batch (BR-06: khong do id tuy y).
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getPatientsFallback")
    @Retry(name = CB)
    public List<PatientDto> getPatients(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String csv = String.join(",", ids.stream().distinct().toList());
        ApiResponse<List<PatientDto>> res = patientRestClient.get()
                .uri(b -> b.path("/api/patients/batch").queryParam("ids", csv).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalTokenClient.token())
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<PatientDto>>>() {});
        return (res != null && res.getData() != null) ? res.getData() : List.<PatientDto>of();
    }

    @SuppressWarnings("unused")
    private List<PatientDto> getPatientsFallback(List<String> ids, Throwable t) {
        return unavailable(t);
    }

    /**
     * Xu ly loi chung cho moi fallback: giu nguyen loi nghiep vu (AppException),
     * con lai quy ve 503 PATIENT_SERVICE_UNAVAILABLE. Luon nem, khong bao gio tra ve null.
     */
    private <T> T unavailable(Throwable t) {
        if (t instanceof AppException ae) {
            throw ae;   // loi nghiep vu (vd 404) -> giu nguyen ma loi
        }
        throw new AppException(ErrorCode.PATIENT_SERVICE_UNAVAILABLE);   // 503
    }
}
