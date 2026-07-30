package com.hehe.medical_record_service.repository;

import com.hehe.medical_record_service.entity.MedicalRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord,String> {
    // Fix N+1 loai A: nap san prescriptionItems trong 1 truy van (LEFT JOIN) thay vi
    // lazy-load tung ho so -> 1+N tro thanh 2 truy van.
    @EntityGraph(attributePaths = "prescriptionItems")
    List<MedicalRecord> getMedicalRecordByPatientId(String patientId);

    Optional<MedicalRecord> findMedicalRecordByAppointmentId(String appointmentId);

    boolean existsByAppointmentId(String appointmentId);
}
