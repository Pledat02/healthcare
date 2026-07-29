package com.hehe.appointment_service.dto.response;

import com.hehe.appointment_service.utils.AppointmentStatus;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AppointmentStatisticsResponse(
        Summary summary,
        Map<AppointmentStatus, Long> statusDistribution,
        List<MonthlyTrend> monthlyTrend,
        List<DoctorRanking> topDoctors,
        List<PatientRanking> frequentPatients
) {
    public record Summary(
            long totalAppointments,
            long validAppointments,
            long completedAppointments,
            long activeDoctors,
            long returningPatients,
            double repeatRate,
            double averageVisits,
            double cancellationRate,
            DayOfWeek busiestDay
    ) {}

    public record MonthlyTrend(
            String month,
            long validAppointments,
            long completedAppointments,
            long cancelledAppointments
    ) {}

    public record DoctorRanking(
            String doctorId,
            long appointmentCount,
            long completedCount
    ) {}

    public record PatientRanking(
            String patientId,
            long completedVisits,
            long distinctDoctorCount,
            Instant lastVisit
    ) {}
}
