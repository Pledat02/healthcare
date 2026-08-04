package com.hehe.doctor_service.service;

import com.hehe.doctor_service.client.KeycloakAdminClient;
import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.entity.Doctor;
import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import com.hehe.doctor_service.mapper.DoctorMapper;
import com.hehe.doctor_service.repository.DoctorRepository;
import com.hehe.doctor_service.utils.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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

/** Unit test DoctorService: US-03 tao bac si + rollback Keycloak khi DB loi, getMe, delete. */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService — US-03 & rollback")
class DoctorServiceTest {

    private static final String KC_ID = "kc-1";

    @Mock DoctorMapper doctorMapper;
    @Mock DoctorRepository doctorRepository;
    @Mock KeycloakAdminClient keycloakAdminClient;
    @Mock SupabaseAvatarStorage avatarStorage;
    @InjectMocks DoctorService service;

    private CreationDoctorRequest request() {
        return CreationDoctorRequest.builder()
                .username("bacsi01").password("Doctor@123").fullName("BS. A").email("a@medibook.local")
                .build();
    }

    @Test
    @DisplayName("US-03: tao bac si -> tao user Keycloak, noi keycloakId roi luu")
    void create_valid_linksKeycloakId() {
        when(keycloakAdminClient.createDoctorUser(any(), any(), any(), any())).thenReturn(KC_ID);
        Doctor entity = new Doctor();
        when(doctorMapper.toEntity(any())).thenReturn(entity);
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(doctorMapper.toResponse(any())).thenReturn(new DoctorResponse());

        service.create(request());

        assertThat(entity.getKeycloakId()).isEqualTo(KC_ID);
        verify(doctorRepository).save(entity);
    }

    @Test
    @DisplayName("US-03 ROLLBACK: luu DB that bai -> xoa user Keycloak vua tao (tranh mo coi)")
    void create_dbFails_rollsBackKeycloakUser() {
        when(keycloakAdminClient.createDoctorUser(any(), any(), any(), any())).thenReturn(KC_ID);
        when(doctorMapper.toEntity(any())).thenReturn(new Doctor());
        when(doctorRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(RuntimeException.class);

        // Diem mau chot: user Keycloak vua tao phai bi go bo
        verify(keycloakAdminClient).deleteUser(KC_ID);
    }

    @Test
    @DisplayName("getMe: khong tim thay ho so theo keycloakId -> DOCTOR_NOT_FOUND")
    void getMe_notFound_throws() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getKeyCloakId).thenReturn(KC_ID);
            when(doctorRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMe())
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("delete: xoa ca tai khoan Keycloak lan ban ghi DB")
    void delete_removesKeycloakAndDb() {
        Doctor d = new Doctor();
        d.setKeycloakId(KC_ID);
        when(doctorRepository.findById("d1")).thenReturn(Optional.of(d));

        service.delete("d1");

        verify(keycloakAdminClient).deleteUser(KC_ID);
        verify(doctorRepository).deleteById("d1");
    }

    // ---------- getByIds (batch): thong nhat findAllById + cap 100 ----------

    @Test
    @DisplayName("Batch qua 100 ID -> TOO_MANY_IDS, khong query DB")
    void getByIds_over100_throwsTooManyIds() {
        assertThatThrownBy(() -> service.getByIds(Collections.nCopies(101, "x")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_IDS);

        verify(doctorRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Batch trong gioi han -> dung findAllById (nhat quan voi patient-service)")
    void getByIds_valid_usesFindAllById() {
        when(doctorRepository.findAllById(any())).thenReturn(List.of(new Doctor()));
        when(doctorMapper.toResponse(any())).thenReturn(new DoctorResponse());

        assertThat(service.getByIds(List.of("d1", "d2"))).hasSize(1);
        verify(doctorRepository).findAllById(any());
    }
}
