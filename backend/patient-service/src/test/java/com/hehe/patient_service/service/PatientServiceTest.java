package com.hehe.patient_service.service;

import com.hehe.patient_service.dto.request.CreationPatientRequest;
import com.hehe.patient_service.dto.response.PatientResponse;
import com.hehe.patient_service.entity.Gender;
import com.hehe.patient_service.entity.Patient;
import com.hehe.patient_service.exception.AppException;
import com.hehe.patient_service.exception.ErrorCode;
import com.hehe.patient_service.helper.SecurityUtil;
import com.hehe.patient_service.mapper.PatientMapper;
import com.hehe.patient_service.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.hehe.patient_service.dto.request.UpdationPatientRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test business rules ho so benh nhan: chong trung (US-02), phan quyen chu so huu (BR-06). */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService — ho so benh nhan & BR-06")
class PatientServiceTest {

    private static final String MY_KC = "kc-me";

    @Mock PatientMapper patientMapper;
    @Mock PatientRepository patientRepository;
    @InjectMocks PatientService service;

    private Patient patientOwnedBy(String keycloakId) {
        Patient p = new Patient();
        p.setId("pat-1");
        p.setKeycloakId(keycloakId);
        return p;
    }

    @Test
    @DisplayName("US-02: tai khoan da co ho so -> PATIENT_ALREADY_EXISTS")
    void create_duplicate_throws() {
        when(patientRepository.existsByKeycloakId(MY_KC)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreationPatientRequest(), MY_KC))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PATIENT_ALREADY_EXISTS);

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hop le: keycloakId gan tu token (khong nhan tu client) roi luu")
    void create_valid_setsKeycloakIdFromToken() {
        when(patientRepository.existsByKeycloakId(MY_KC)).thenReturn(false);
        Patient entity = new Patient();
        when(patientMapper.toEntity(any())).thenReturn(entity);
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientMapper.toResponse(any())).thenReturn(new PatientResponse());

        service.create(new CreationPatientRequest(), MY_KC);

        assertThat(entity.getKeycloakId()).isEqualTo(MY_KC);
        verify(patientRepository).save(entity);
    }

    @Test
    @DisplayName("getMe: chua co ho so -> PATIENT_NOT_FOUND")
    void getMe_notFound_throws() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentKeyCloakId).thenReturn(MY_KC);
            when(patientRepository.findByKeycloakId(MY_KC)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMe())
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PATIENT_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("BR-06: xem ho so nguoi khac (khong admin/doctor/chinh chu) -> FORBIDDEN")
    void getOne_notOwner_throws() {
        when(patientRepository.findById("pat-1")).thenReturn(Optional.of(patientOwnedBy("kc-NGUOI-KHAC")));
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(() -> SecurityUtil.hasRole("DOCTOR")).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentKeyCloakId).thenReturn(MY_KC);

            assertThatThrownBy(() -> service.getOne("pat-1"))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("BR-06: chinh chu xem ho so cua minh -> OK")
    void getOne_owner_ok() {
        when(patientRepository.findById("pat-1")).thenReturn(Optional.of(patientOwnedBy(MY_KC)));
        when(patientMapper.toResponse(any())).thenReturn(new PatientResponse());
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(() -> SecurityUtil.hasRole("DOCTOR")).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentKeyCloakId).thenReturn(MY_KC);

            assertThat(service.getOne("pat-1")).isNotNull();
        }
    }

    @Test
    @DisplayName("getAll: khong phai ADMIN -> FORBIDDEN")
    void getAll_notAdmin_throws() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.getAll(0, 20, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(patientRepository, never()).search(any(), any(), any());
    }

    @Test
    @DisplayName("ADMIN: danh sach benh nhan duoc phan trang va gioi han size toi da 100")
    void getAll_asAdmin_returnsPage() {
        Patient patient = patientOwnedBy("kc-x");
        when(patientRepository.search(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(patient)));
        when(patientMapper.toResponse(patient)).thenReturn(new PatientResponse());
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            var result = service.getAll(-1, 200, "  an  ", Gender.MALE);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(patientRepository).search(
                    org.mockito.ArgumentMatchers.eq("an"),
                    org.mockito.ArgumentMatchers.eq(Gender.MALE),
                    org.mockito.ArgumentMatchers.argThat(pageable ->
                            pageable.getPageNumber() == 0 && pageable.getPageSize() == 100));
        }
    }

    // ---------- getByIds (batch): BR-06 + gioi han 100 ID ----------

    @Test
    @DisplayName("BR-06: PATIENT goi batch -> FORBIDDEN (khong do duoc ID nguoi khac)")
    void getByIds_asPatient_throwsForbidden() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.getByIds(List.of("p1", "p2")))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(patientRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("BR-06: DOCTOR goi batch THANG -> FORBIDDEN (batch la API noi bo, chi ADMIN/service)")
    void getByIds_asDoctor_throwsForbidden() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.getByIds(List.of("p1")))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(patientRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Batch qua 100 ID -> TOO_MANY_IDS (400)")
    void getByIds_over100_throwsTooManyIds() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            assertThatThrownBy(() -> service.getByIds(Collections.nCopies(101, "x")))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TOO_MANY_IDS);
        }
        verify(patientRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("ADMIN goi batch trong gioi han -> OK")
    void getByIds_asAdmin_ok() {
        when(patientRepository.findAllById(any())).thenReturn(List.of(patientOwnedBy("kc-x")));
        when(patientMapper.toResponse(any())).thenReturn(new PatientResponse());
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            assertThat(service.getByIds(List.of("p1"))).hasSize(1);
        }
    }

    // ---------- delete: chi ADMIN (defense-in-depth) ----------

    @Test
    @DisplayName("Xoa khi khong phai ADMIN -> FORBIDDEN, khong xoa DB")
    void delete_asNonAdmin_throwsForbidden() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.delete("pat-1"))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(patientRepository, never()).delete(any());
    }

    @Test
    @DisplayName("ADMIN xoa -> delete + flush de database cascade truoc khi tra response")
    void delete_asAdmin_ok() {
        Patient patient = patientOwnedBy("kc-x");
        when(patientRepository.findById("pat-1")).thenReturn(Optional.of(patient));
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            service.delete("pat-1");
        }
        verify(patientRepository).delete(patient);
        verify(patientRepository).flush();
    }

    @Test
    @DisplayName("ADMIN xoa ID khong ton tai -> PATIENT_NOT_FOUND")
    void delete_asAdmin_notFound() {
        when(patientRepository.findById("missing")).thenReturn(Optional.empty());
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            assertThatThrownBy(() -> service.delete("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PATIENT_NOT_FOUND);
        }
        verify(patientRepository, never()).delete(any());
        verify(patientRepository, never()).flush();
    }

    // ---------- update (BR-06 GHI): chi ADMIN hoac chinh chu, DOCTOR khong duoc sua ----------

    @Test
    @DisplayName("BR-06 GHI: DOCTOR sua ho so benh nhan -> FORBIDDEN (bac si khong duoc sua)")
    void update_asDoctor_throwsForbidden() {
        when(patientRepository.findById("pat-1")).thenReturn(Optional.of(patientOwnedBy("kc-benhnhan")));
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentKeyCloakId).thenReturn("kc-bacsi"); // khac chu ho so

            assertThatThrownBy(() -> service.update("pat-1", new UpdationPatientRequest()))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chinh chu sua ho so cua minh -> OK")
    void update_asOwner_ok() {
        when(patientRepository.findById("pat-1")).thenReturn(Optional.of(patientOwnedBy(MY_KC)));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientMapper.toResponse(any())).thenReturn(new PatientResponse());
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentKeyCloakId).thenReturn(MY_KC);

            assertThat(service.update("pat-1", new UpdationPatientRequest())).isNotNull();
        }
        verify(patientRepository).save(any());
    }
}
