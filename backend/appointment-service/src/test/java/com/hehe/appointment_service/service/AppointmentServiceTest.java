package com.hehe.appointment_service.service;

import com.hehe.appointment_service.client.DoctorClient;
import com.hehe.appointment_service.client.NotificationClient;
import com.hehe.appointment_service.client.PatientClient;
import com.hehe.appointment_service.dto.request.CreationAppointmentRequest;
import com.hehe.appointment_service.dto.request.UpdateAppointmentRequest;
import com.hehe.appointment_service.dto.response.AppointmentResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
import com.hehe.appointment_service.dto.response.PatientDto;
import com.hehe.appointment_service.entity.Appointment;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.mapper.AppointmentMapper;
import com.hehe.appointment_service.repository.AppointmentRepository;
import com.hehe.appointment_service.repository.RatingRepository;
import com.hehe.appointment_service.utils.AppointmentStatus;
import com.hehe.appointment_service.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test cho business rules dat lich (BR-01/02/03) + luoi chan race condition.
 * Mock toan bo dependency (client, repo, mapper) -> chay nhanh, khong can DB/Keycloak.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService — business rules dat lich")
class AppointmentServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String DOCTOR_ID = "doc-1";

    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentMapper appointmentMapper;
    @Mock DoctorClient doctorClient;
    @Mock PatientClient patientClient;
    @Mock NotificationClient notificationClient;
    @Mock RatingRepository ratingRepository;
    @InjectMocks AppointmentService service;

    @BeforeEach
    void setUp() {
        // durationMinutes la @Value @NonFinal -> set thu cong khi khong co Spring context
        ReflectionTestUtils.setField(service, "durationMinutes", 30);
    }

    // ---------- helpers ----------
    private CreationAppointmentRequest requestAt(Instant time) {
        return CreationAppointmentRequest.builder()
                .doctorId(DOCTOR_ID).appointmentTime(time).reason("Dau dau").build();
    }

    /** Instant ung voi gio VN cho truoc, vao mot ngay tuong lai (qua BR-02). */
    private Instant futureAtVietnamTime(int hour, int minute) {
        return LocalDate.now().plusYears(1).atTime(hour, minute).atZone(CLINIC_ZONE).toInstant();
    }

    private DoctorDto doctorWorking(LocalTime start, LocalTime end) {
        return DoctorDto.builder().id(DOCTOR_ID).fullName("BS. A").specialization("Noi")
                .workStartTime(start).workEndTime(end).build();
    }

    private PatientDto patient(String id) {
        PatientDto p = new PatientDto();
        p.setId(id);
        p.setFullName("BN B");
        p.setEmail("b@example.com");
        return p;
    }

    // ---------- tests ----------

    @Test
    @DisplayName("BR-02: dat lich trong qua khu -> APPOINTMENT_TIME_IN_PAST, khong goi service nao khac")
    void create_pastTime_throwsInPast() {
        var request = requestAt(Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPOINTMENT_TIME_IN_PAST);

        // Loi som -> khong cham toi doctor/patient/DB/notification
        verifyNoInteractions(doctorClient, appointmentRepository, patientClient, notificationClient);
    }

    @Test
    @DisplayName("BR-03: gio hen ngoai gio lam viec -> OUTSIDE_WORKING_HOURS, khong kiem trung lich")
    void create_outsideWorkingHours_throws() {
        var request = requestAt(futureAtVietnamTime(6, 0)); // 06:00, truoc gio mo cua 08:00
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OUTSIDE_WORKING_HOURS);

        verify(appointmentRepository, never()).isConflict(anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("BR-01: bac si da co lich trung gio -> APPOINTMENT_CONFLICT, khong luu")
    void create_conflict_throwsConflict() {
        var request = requestAt(futureAtVietnamTime(10, 0));
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(appointmentRepository.isConflict(eq(DOCTOR_ID), any(), eq(30))).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPOINTMENT_CONFLICT);

        verify(appointmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationClient);
    }

    @Test
    @DisplayName("Race: unique index bi vi pham khi luu -> APPOINTMENT_CONFLICT, KHONG gui mail")
    void create_uniqueIndexViolation_throwsConflict() {
        var time = futureAtVietnamTime(10, 0);
        var request = requestAt(time);
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(appointmentRepository.isConflict(eq(DOCTOR_ID), any(), eq(30))).thenReturn(false);
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));

        Appointment entity = new Appointment();
        entity.setDoctorId(DOCTOR_ID);
        entity.setAppointmentTime(time);
        when(appointmentMapper.toEntity(request)).thenReturn(entity);
        when(appointmentRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key uq_doctor_slot"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPOINTMENT_CONFLICT);

        // Mail chi duoc gui khi luu THANH CONG
        verifyNoInteractions(notificationClient);
    }

    @Test
    @DisplayName("Hop le: qua BR-01/02/03 -> luu lich + gui thong bao, patientId & duration set tu server")
    void create_valid_savesAndNotifies() {
        var time = futureAtVietnamTime(10, 0);
        var request = requestAt(time);
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(appointmentRepository.isConflict(eq(DOCTOR_ID), any(), eq(30))).thenReturn(false);
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));

        Appointment entity = new Appointment();
        entity.setDoctorId(DOCTOR_ID);
        entity.setAppointmentTime(time);
        when(appointmentMapper.toEntity(request)).thenReturn(entity);
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientClient.getPatient("pat-1")).thenReturn(patient("pat-1")); // trong notify()
        AppointmentResponse response = new AppointmentResponse();
        when(appointmentMapper.toResponse(any())).thenReturn(response);

        var result = service.create(request);

        assertThat(result).isSameAs(response);
        verify(appointmentRepository).saveAndFlush(any());
        verify(notificationClient, times(1)).send(any()); // gui mail dung 1 lan
        // Server tu quyet, khong nhan tu client (muc 17.1 PRD)
        assertThat(entity.getDurationMinutes()).isEqualTo(30);
        assertThat(entity.getPatientId()).isEqualTo("pat-1");
    }

    @Test
    @DisplayName("BR: benh nhan tu trung gio voi lich khac cua minh (bac si khac) -> PATIENT_TIME_CONFLICT, khong luu")
    void create_patientOverlap_throwsConflict() {
        var time = futureAtVietnamTime(10, 0);
        var request = requestAt(time);
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(appointmentRepository.isConflict(eq(DOCTOR_ID), any(), eq(30))).thenReturn(false);
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));
        when(appointmentMapper.toEntity(request)).thenReturn(new Appointment());
        // Lich moi chua co id -> currentId = "" (khong loai tru gi)
        when(appointmentRepository.existsPatientOverlap(eq("pat-1"), any(), eq(30), eq("")))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PATIENT_TIME_CONFLICT);

        // Chan truoc khi cham DB/mail
        verify(appointmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationClient);
    }

    // ---------- cancel() : BR-06 (chu so huu) + BR-04 (khong sua lich da COMPLETED) ----------

    private Appointment existingAppointment(String patientId, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId("appt-1");
        a.setPatientId(patientId);
        a.setDoctorId(DOCTOR_ID);
        a.setStatus(status);
        a.setAppointmentTime(futureAtVietnamTime(10, 0));
        return a;
    }

    @Test
    @DisplayName("BR-06: huy lich khong phai cua minh -> FORBIDDEN")
    void cancel_notOwner_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-OWNER", AppointmentStatus.CONFIRMED)));
        when(patientClient.getPatient()).thenReturn(patient("pat-OTHER"));

        assertThatThrownBy(() -> service.cancel("appt-1", null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-04: huy lich da COMPLETED -> CANNOT_MODIFY_COMPLETED")
    void cancel_completed_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-1", AppointmentStatus.COMPLETED)));
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));

        assertThatThrownBy(() -> service.cancel("appt-1", null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_COMPLETED);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-06: chu lich huy hop le -> chuyen CANCELLED + gui thong bao")
    void cancel_valid_cancelsAndNotifies() {
        Appointment appt = existingAppointment("pat-1", AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appt));
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));
        when(patientClient.getPatient("pat-1")).thenReturn(patient("pat-1"));           // trong notify()
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));    // trong notify()

        boolean result = service.cancel("appt-1", null);

        assertThat(result).isTrue();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appt);
        verify(notificationClient).send(any());   // gui su kien huy -> notification tu huy lich nhac
    }

    // ---------- update() : doi gio hen (BR-06, BR-04, BR-01) ----------

    private DoctorDto doctor(String id) {
        return DoctorDto.builder().id(id).build();
    }

    private UpdateAppointmentRequest updateRequest(Instant time) {
        return UpdateAppointmentRequest.builder()
                .doctorId(DOCTOR_ID).appointmentTime(time).reason("Doi gio").build();
    }

    @Test
    @DisplayName("BR-06: doi lich khong phai cua minh -> FORBIDDEN")
    void update_notOwner_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-OWNER", AppointmentStatus.CONFIRMED)));
        when(patientClient.getPatient()).thenReturn(patient("pat-OTHER"));

        assertThatThrownBy(() -> service.update("appt-1", updateRequest(futureAtVietnamTime(11, 0))))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("BR-04: doi lich da COMPLETED -> CANNOT_MODIFY_COMPLETED")
    void update_completed_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-1", AppointmentStatus.COMPLETED)));
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));

        assertThatThrownBy(() -> service.update("appt-1", updateRequest(futureAtVietnamTime(11, 0))))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_COMPLETED);

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Doi lich hop le -> luu + gui thong bao RESCHEDULED")
    void update_valid_reschedulesAndNotifies() {
        Appointment appt = existingAppointment("pat-1", AppointmentStatus.CONFIRMED);
        var time = futureAtVietnamTime(11, 0);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appt));
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(appointmentRepository.isConflictOnUpdate(eq(DOCTOR_ID), any(), eq(30), eq("appt-1")))
                .thenReturn(false);
        when(appointmentMapper.updateEntity(any(), any())).thenReturn(appt);
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientClient.getPatient("pat-1")).thenReturn(patient("pat-1"));
        when(appointmentMapper.toResponse(any())).thenReturn(new AppointmentResponse());

        service.update("appt-1", updateRequest(time));

        verify(appointmentRepository).saveAndFlush(appt);
        verify(notificationClient).send(any());
    }

    @Test
    @DisplayName("BR: doi gio de len mot lich khac cua chinh minh -> PATIENT_TIME_CONFLICT, khong luu")
    void update_patientOverlap_throwsConflict() {
        Appointment appt = existingAppointment("pat-1", AppointmentStatus.CONFIRMED);
        var time = futureAtVietnamTime(11, 0);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appt));
        when(patientClient.getPatient()).thenReturn(patient("pat-1"));
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        when(appointmentRepository.isConflictOnUpdate(eq(DOCTOR_ID), any(), eq(30), eq("appt-1")))
                .thenReturn(false);
        // Loai tru chinh lich dang doi (appt-1), nhung van de len mot lich khac
        when(appointmentRepository.existsPatientOverlap(eq("pat-1"), any(), eq(30), eq("appt-1")))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update("appt-1", updateRequest(time)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PATIENT_TIME_CONFLICT);

        verify(appointmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationClient);
    }

    // ---------- confirm() : bac si/admin xac nhan ----------

    @Test
    @DisplayName("Xac nhan: khong phai bac si buoi do va khong ADMIN -> FORBIDDEN")
    void confirm_notOwnerNorAdmin_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-1", AppointmentStatus.PENDING)));
        when(doctorClient.getMe()).thenReturn(doctor("doc-KHAC"));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> service.confirm("appt-1"))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xac nhan lich da CANCELLED -> CANNOT_MODIFY_COMPLETED")
    void confirm_cancelled_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-1", AppointmentStatus.CANCELLED)));
        when(doctorClient.getMe()).thenReturn(doctor(DOCTOR_ID)); // dung bac si -> qua check quyen

        assertThatThrownBy(() -> service.confirm("appt-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_COMPLETED);
    }

    @Test
    @DisplayName("Bac si xac nhan hop le -> chuyen CONFIRMED + gui thong bao")
    void confirm_valid_confirmsAndNotifies() {
        Appointment appt = existingAppointment("pat-1", AppointmentStatus.PENDING);
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appt));
        when(doctorClient.getMe()).thenReturn(doctor(DOCTOR_ID));
        when(patientClient.getPatient("pat-1")).thenReturn(patient("pat-1")); // notify()
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));

        assertThat(service.confirm("appt-1")).isTrue();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(notificationClient).send(any());
    }

    // ---------- complete() : bac si danh dau da kham xong ----------

    @Test
    @DisplayName("Hoan thanh: khong phai bac si buoi do -> FORBIDDEN")
    void complete_notTreatingDoctor_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-1", AppointmentStatus.CONFIRMED)));
        when(doctorClient.getMe()).thenReturn(doctor("doc-KHAC"));

        assertThatThrownBy(() -> service.complete("appt-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("Hoan thanh lich chua CONFIRMED -> CANNOT_MODIFY_COMPLETED")
    void complete_notConfirmed_throws() {
        when(appointmentRepository.findById("appt-1"))
                .thenReturn(Optional.of(existingAppointment("pat-1", AppointmentStatus.PENDING)));
        when(doctorClient.getMe()).thenReturn(doctor(DOCTOR_ID));

        assertThatThrownBy(() -> service.complete("appt-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_COMPLETED);
    }

    @Test
    @DisplayName("Bac si hoan thanh hop le -> chuyen COMPLETED + gui thong bao")
    void complete_valid_completesAndNotifies() {
        Appointment appt = existingAppointment("pat-1", AppointmentStatus.CONFIRMED);
        appt.setAppointmentTime(Instant.now().minusSeconds(3600)); // da toi gio hen -> duoc hoan thanh
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appt));
        when(doctorClient.getMe()).thenReturn(doctor(DOCTOR_ID));
        when(patientClient.getPatient("pat-1")).thenReturn(patient("pat-1")); // notify()
        when(doctorClient.getDoctor(DOCTOR_ID))
                .thenReturn(doctorWorking(LocalTime.of(8, 0), LocalTime.of(17, 0)));

        assertThat(service.complete("appt-1")).isTrue();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(notificationClient).send(any());
    }

    @Test
    @DisplayName("complete: chua toi gio hen -> chan (TOO_EARLY_TO_COMPLETE)")
    void complete_beforeAppointmentTime_throws() {
        Appointment appt = existingAppointment("pat-1", AppointmentStatus.CONFIRMED);
        appt.setAppointmentTime(Instant.now().plusSeconds(3600)); // gio hen o tuong lai
        when(appointmentRepository.findById("appt-1")).thenReturn(Optional.of(appt));
        when(doctorClient.getMe()).thenReturn(doctor(DOCTOR_ID));

        assertThatThrownBy(() -> service.complete("appt-1"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_EARLY_TO_COMPLETE);
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }
}
