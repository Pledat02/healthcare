package com.hehe.appointment_service.service;

import com.hehe.appointment_service.dto.response.AppointmentStatisticsResponse;
import com.hehe.appointment_service.entity.Appointment;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.repository.AppointmentRepository;
import com.hehe.appointment_service.utils.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AppointmentStatisticsService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_RANKING_LIMIT = 100;
    private static final int MONTHS_ON_CHART = 8;

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public AppointmentStatisticsResponse getStatistics(LocalDate from, LocalDate to, int limit) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        int rankingLimit = Math.max(1, Math.min(limit, MAX_RANKING_LIMIT));
        List<Appointment> appointments = findInRange(from, to);

        EnumMap<AppointmentStatus, Long> statusDistribution = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values()) statusDistribution.put(status, 0L);

        Map<String, DoctorAccumulator> doctors = new HashMap<>();
        Map<String, PatientAccumulator> patients = new HashMap<>();
        Map<DayOfWeek, Long> visitsByWeekday = new EnumMap<>(DayOfWeek.class);
        TreeMap<YearMonth, MonthlyAccumulator> months = new TreeMap<>();

        long validAppointments = 0;
        long completedAppointments = 0;

        for (Appointment appointment : appointments) {
            AppointmentStatus status = appointment.getStatus();
            if (status == null) continue;
            statusDistribution.merge(status, 1L, Long::sum);

            Instant appointmentTime = appointment.getAppointmentTime();
            if (appointmentTime != null) {
                YearMonth month = YearMonth.from(appointmentTime.atZone(CLINIC_ZONE));
                MonthlyAccumulator monthly = months.computeIfAbsent(month, ignored -> new MonthlyAccumulator());
                if (status == AppointmentStatus.CANCELLED) monthly.cancelled++;
                else monthly.valid++;
                if (status == AppointmentStatus.COMPLETED) monthly.completed++;
            }

            if (status != AppointmentStatus.CANCELLED) {
                validAppointments++;
                if (appointment.getDoctorId() != null) {
                    DoctorAccumulator doctor = doctors.computeIfAbsent(
                            appointment.getDoctorId(), ignored -> new DoctorAccumulator());
                    doctor.appointments++;
                    if (status == AppointmentStatus.COMPLETED) doctor.completed++;
                }
            }

            if (status == AppointmentStatus.COMPLETED) {
                completedAppointments++;
                if (appointmentTime != null) {
                    DayOfWeek weekday = appointmentTime.atZone(CLINIC_ZONE).getDayOfWeek();
                    visitsByWeekday.merge(weekday, 1L, Long::sum);
                }
                if (appointment.getPatientId() != null) {
                    PatientAccumulator patient = patients.computeIfAbsent(
                            appointment.getPatientId(), ignored -> new PatientAccumulator());
                    patient.visits++;
                    if (appointment.getDoctorId() != null) patient.doctorIds.add(appointment.getDoctorId());
                    if (appointmentTime != null && (patient.lastVisit == null || appointmentTime.isAfter(patient.lastVisit))) {
                        patient.lastVisit = appointmentTime;
                    }
                }
            }
        }

        long returningPatients = patients.values().stream().filter(patient -> patient.visits >= 2).count();
        long cancelledAppointments = statusDistribution.get(AppointmentStatus.CANCELLED);
        DayOfWeek busiestDay = visitsByWeekday.entrySet().stream()
                .max(Map.Entry.<DayOfWeek, Long>comparingByValue()
                        .thenComparing(entry -> entry.getKey().getValue(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);

        AppointmentStatisticsResponse.Summary summary = new AppointmentStatisticsResponse.Summary(
                appointments.size(),
                validAppointments,
                completedAppointments,
                doctors.size(),
                returningPatients,
                percentage(returningPatients, patients.size()),
                average(completedAppointments, patients.size()),
                percentage(cancelledAppointments, appointments.size()),
                busiestDay
        );

        return new AppointmentStatisticsResponse(
                summary,
                statusDistribution,
                monthlyTrend(months),
                topDoctors(doctors, rankingLimit),
                frequentPatients(patients, rankingLimit)
        );
    }

    private List<Appointment> findInRange(LocalDate from, LocalDate to) {
        Instant start = from == null ? null : from.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant end = to == null ? null : to.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
        if (start != null && end != null) {
            return appointmentRepository.findByAppointmentTimeGreaterThanEqualAndAppointmentTimeLessThan(start, end);
        }
        if (start != null) return appointmentRepository.findByAppointmentTimeGreaterThanEqual(start);
        if (end != null) return appointmentRepository.findByAppointmentTimeLessThan(end);
        return appointmentRepository.findAll();
    }

    private List<AppointmentStatisticsResponse.MonthlyTrend> monthlyTrend(
            TreeMap<YearMonth, MonthlyAccumulator> months) {
        List<Map.Entry<YearMonth, MonthlyAccumulator>> entries = new ArrayList<>(months.entrySet());
        int fromIndex = Math.max(0, entries.size() - MONTHS_ON_CHART);
        return entries.subList(fromIndex, entries.size()).stream()
                .map(entry -> new AppointmentStatisticsResponse.MonthlyTrend(
                        entry.getKey().toString(),
                        entry.getValue().valid,
                        entry.getValue().completed,
                        entry.getValue().cancelled))
                .toList();
    }

    private List<AppointmentStatisticsResponse.DoctorRanking> topDoctors(
            Map<String, DoctorAccumulator> doctors, int limit) {
        return doctors.entrySet().stream()
                .map(entry -> new AppointmentStatisticsResponse.DoctorRanking(
                        entry.getKey(), entry.getValue().appointments, entry.getValue().completed))
                .sorted(Comparator.comparingLong(AppointmentStatisticsResponse.DoctorRanking::appointmentCount).reversed()
                        .thenComparing(Comparator.comparingLong(
                                AppointmentStatisticsResponse.DoctorRanking::completedCount).reversed())
                        .thenComparing(AppointmentStatisticsResponse.DoctorRanking::doctorId))
                .limit(limit)
                .toList();
    }

    private List<AppointmentStatisticsResponse.PatientRanking> frequentPatients(
            Map<String, PatientAccumulator> patients, int limit) {
        return patients.entrySet().stream()
                .map(entry -> new AppointmentStatisticsResponse.PatientRanking(
                        entry.getKey(),
                        entry.getValue().visits,
                        entry.getValue().doctorIds.size(),
                        entry.getValue().lastVisit))
                .sorted(Comparator.comparingLong(AppointmentStatisticsResponse.PatientRanking::completedVisits).reversed()
                        .thenComparing(AppointmentStatisticsResponse.PatientRanking::lastVisit,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AppointmentStatisticsResponse.PatientRanking::patientId))
                .limit(limit)
                .toList();
    }

    private double percentage(long value, long total) {
        if (total == 0) return 0;
        return Math.round((value * 1000.0) / total) / 10.0;
    }

    private double average(long value, long total) {
        if (total == 0) return 0;
        return Math.round((value * 10.0) / total) / 10.0;
    }

    private static final class DoctorAccumulator {
        long appointments;
        long completed;
    }

    private static final class PatientAccumulator {
        long visits;
        Instant lastVisit;
        Set<String> doctorIds = new HashSet<>();
    }

    private static final class MonthlyAccumulator {
        long valid;
        long completed;
        long cancelled;
    }
}
