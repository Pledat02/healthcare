package com.hehe.medical_record_service.service;

import com.hehe.medical_record_service.client.AppointmentClient;
import com.hehe.medical_record_service.client.DoctorClient;
import com.hehe.medical_record_service.client.PatientClient;
import com.hehe.medical_record_service.dto.request.CreationMedicalRecordRequest;
import com.hehe.medical_record_service.dto.response.AppointmentDto;
import com.hehe.medical_record_service.dto.response.DoctorDto;
import com.hehe.medical_record_service.dto.response.MedicalRecordResponse;
import com.hehe.medical_record_service.entity.MedicalRecord;
import com.hehe.medical_record_service.exception.AppException;
import com.hehe.medical_record_service.exception.ErrorCode;
import com.hehe.medical_record_service.mapper.MedicalRecordMapper;
import com.hehe.medical_record_service.repository.MedicalRecordRepository;
import com.hehe.medical_record_service.utils.AppointmentStatus;
import com.hehe.medical_record_service.utils.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test business rules ghi ho so kham: US-10, BR-05, 1 lich <-> 1 ho so, BR-06. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalRecordService — business rules ghi ho so kham")
class MedicalRecordServiceTest {

    private static final String APPT_ID = "appt-1";
    private static final String DOCTOR_ID = "doc-1";
    private static final String PATIENT_ID = "pat-9";

    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock MedicalRecordMapper medicalRecordMapper;
    @Mock AppointmentClient appointmentClient;
    @Mock DoctorClient doctorClient;
    @Mock PatientClient patientClient;
    @InjectMocks MedicalRecordService service;

    private CreationMedicalRecordRequest request() {
        CreationMedicalRecordRequest r = new CreationMedicalRecordRequest();
        r.setAppointmentId(APPT_ID);
        r.setDiagnosis("Viem hong");
        return r;
    }

    private AppointmentDto appointment(AppointmentStatus status) {
        return AppointmentDto.builder()
                .patientId(PATIENT_ID).doctorId(DOCTOR_ID).status(status).build();
    }

    private DoctorDto me(String id) {
        return DoctorDto.builder().id(id).build();
    }

    @Test
    @DisplayName("US-10: khong phai bac si kham buoi do -> NOT_THE_TREATING_DOCTOR")
    void create_notTreatingDoctor_throws() {
        when(appointmentClient.getAppointment(APPT_ID)).thenReturn(appointment(AppointmentStatus.COMPLETED));
        when(doctorClient.getMe()).thenReturn(me("doc-KHAC"));  // bac si khac

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_THE_TREATING_DOCTOR);

        verify(medicalRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-05: lich chua COMPLETED -> APPOINTMENT_NOT_COMPLETED")
    void create_appointmentNotCompleted_throws() {
        when(appointmentClient.getAppointment(APPT_ID)).thenReturn(appointment(AppointmentStatus.CONFIRMED));
        when(doctorClient.getMe()).thenReturn(me(DOCTOR_ID));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPOINTMENT_NOT_COMPLETED);

        verify(medicalRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("1 lich <-> 1 ho so: da co ho so -> MEDICAL_RECORD_ALREADY_EXISTS")
    void create_duplicate_throws() {
        when(appointmentClient.getAppointment(APPT_ID)).thenReturn(appointment(AppointmentStatus.COMPLETED));
        when(doctorClient.getMe()).thenReturn(me(DOCTOR_ID));
        when(medicalRecordRepository.existsByAppointmentId(APPT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_RECORD_ALREADY_EXISTS);

        verify(medicalRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hop le: patientId & doctorId lay tu LICH HEN (khong nhan tu client)")
    void create_valid_setsIdsFromAppointment() {
        when(appointmentClient.getAppointment(APPT_ID)).thenReturn(appointment(AppointmentStatus.COMPLETED));
        when(doctorClient.getMe()).thenReturn(me(DOCTOR_ID));
        when(medicalRecordRepository.existsByAppointmentId(APPT_ID)).thenReturn(false);

        MedicalRecord entity = new MedicalRecord();
        when(medicalRecordMapper.toEntity(any())).thenReturn(entity);
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MedicalRecordResponse response = new MedicalRecordResponse();
        when(medicalRecordMapper.toResponse(any())).thenReturn(response);

        var result = service.create(request());

        assertThat(result).isSameAs(response);
        // ID lay tu lich hen -> chong ghi ho so cho benh nhan khac
        assertThat(entity.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(entity.getDoctorId()).isEqualTo(DOCTOR_ID);
        verify(medicalRecordRepository).save(entity);
    }

    @Test
    @DisplayName("BR-06: xem lich su benh nhan khac ma khong phai ADMIN/DOCTOR -> FORBIDDEN")
    void getHistory_notAdminNorDoctor_throws() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
            mocked.when(() -> SecurityUtils.hasRole("DOCTOR")).thenReturn(false);

            assertThatThrownBy(() -> service.getHistoryPatientMedicalRecord(PATIENT_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(medicalRecordRepository, never()).getMedicalRecordByPatientId(any());
    }
}
