package com.hehe.appointment_service.repository;

import com.hehe.appointment_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment,String> {

    @NativeQuery("SELECT EXISTS ( " +
            "  SELECT 1 FROM appointments a " +
            "  WHERE a.doctor_id = :doctorId " +
            "    AND a.status <> 'CANCELLED' " +
            "    AND a.appointment_time < :appointmentTime + (:durationMinutes * INTERVAL '1 minute') " +
            "    AND :appointmentTime < a.appointment_time + (a.duration_minutes * INTERVAL '1 minute') " +
            ")")
    boolean isConflict(@Param("doctorId") String doctorId,
                       @Param("appointmentTime") Instant appointmentTime,
                       @Param("durationMinutes") int durationMinutes);

    @NativeQuery("SELECT EXISTS ( " +
            "  SELECT 1 FROM appointments a " +
            "  WHERE a.doctor_id = :doctorId " +
            "    AND a.id <> :currentId " +          // ← LOẠI TRỪ chính nó
            "    AND a.status <> 'CANCELLED' " +
            "    AND a.appointment_time < :appointmentTime + (:durationMinutes * INTERVAL '1 minute') " +
            "    AND :appointmentTime < a.appointment_time + (a.duration_minutes * INTERVAL '1 minute') " +
            ")")
    boolean isConflictOnUpdate(@Param("doctorId") String doctorId,
                               @Param("appointmentTime") Instant appointmentTime,
                               @Param("durationMinutes") int durationMinutes,
                               @Param("currentId") String currentId);

    // Lay lich cua bac si trong 1 ngay: appointmentTime nam trong [dau ngay, dau ngay hom sau)
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(String doctorId, Instant start, Instant end);
    List<Appointment> findByPatientId(String patientId);
}
