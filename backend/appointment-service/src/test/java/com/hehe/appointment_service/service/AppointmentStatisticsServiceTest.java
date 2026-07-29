package com.hehe.appointment_service.service;

import com.hehe.appointment_service.dto.response.AppointmentStatisticsResponse;
import com.hehe.appointment_service.entity.Appointment;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.repository.AppointmentRepository;
import com.hehe.appointment_service.utils.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentStatisticsServiceTest {

    @Mock
    AppointmentRepository appointmentRepository;

    @InjectMocks
    AppointmentStatisticsService statisticsService;

    @Test
    void aggregatesDashboardMetricsAndRankings() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        Instant start = Instant.parse("2026-06-30T17:00:00Z");
        Instant end = Instant.parse("2026-07-31T17:00:00Z");
        List<Appointment> appointments = List.of(
                appointment("patient-1", "doctor-1", "2026-07-01T02:00:00Z", AppointmentStatus.COMPLETED),
                appointment("patient-1", "doctor-1", "2026-07-08T02:00:00Z", AppointmentStatus.COMPLETED),
                appointment("patient-2", "doctor-2", "2026-07-10T02:00:00Z", AppointmentStatus.CONFIRMED),
                appointment("patient-2", "doctor-2", "2026-07-11T02:00:00Z", AppointmentStatus.CANCELLED)
        );
        when(appointmentRepository.findByAppointmentTimeGreaterThanEqualAndAppointmentTimeLessThan(start, end))
                .thenReturn(appointments);

        AppointmentStatisticsResponse result = statisticsService.getStatistics(from, to, 10);

        assertThat(result.summary().totalAppointments()).isEqualTo(4);
        assertThat(result.summary().validAppointments()).isEqualTo(3);
        assertThat(result.summary().completedAppointments()).isEqualTo(2);
        assertThat(result.summary().activeDoctors()).isEqualTo(2);
        assertThat(result.summary().returningPatients()).isEqualTo(1);
        assertThat(result.summary().repeatRate()).isEqualTo(100.0);
        assertThat(result.summary().averageVisits()).isEqualTo(2.0);
        assertThat(result.summary().cancellationRate()).isEqualTo(25.0);
        assertThat(result.summary().busiestDay()).isEqualTo(DayOfWeek.WEDNESDAY);

        assertThat(result.statusDistribution().get(AppointmentStatus.COMPLETED)).isEqualTo(2);
        assertThat(result.statusDistribution().get(AppointmentStatus.CONFIRMED)).isEqualTo(1);
        assertThat(result.statusDistribution().get(AppointmentStatus.CANCELLED)).isEqualTo(1);
        assertThat(result.statusDistribution().get(AppointmentStatus.PENDING)).isZero();

        assertThat(result.monthlyTrend()).singleElement().satisfies(month -> {
            assertThat(month.month()).isEqualTo("2026-07");
            assertThat(month.validAppointments()).isEqualTo(3);
            assertThat(month.completedAppointments()).isEqualTo(2);
            assertThat(month.cancelledAppointments()).isEqualTo(1);
        });
        assertThat(result.topDoctors()).extracting(AppointmentStatisticsResponse.DoctorRanking::doctorId)
                .containsExactly("doctor-1", "doctor-2");
        assertThat(result.topDoctors().get(0).appointmentCount()).isEqualTo(2);
        assertThat(result.frequentPatients()).singleElement().satisfies(patient -> {
            assertThat(patient.patientId()).isEqualTo("patient-1");
            assertThat(patient.completedVisits()).isEqualTo(2);
            assertThat(patient.distinctDoctorCount()).isEqualTo(1);
        });

        verify(appointmentRepository)
                .findByAppointmentTimeGreaterThanEqualAndAppointmentTimeLessThan(start, end);
    }

    @Test
    void rejectsReversedDateRange() {
        assertThatThrownBy(() -> statisticsService.getStatistics(
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1), 10))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_DATE_RANGE));

        verifyNoInteractions(appointmentRepository);
    }

    private Appointment appointment(String patientId, String doctorId, String time, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentTime(Instant.parse(time));
        appointment.setStatus(status);
        return appointment;
    }
}
