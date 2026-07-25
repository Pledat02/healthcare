package com.hehe.medical_record_service.repository;

import com.hehe.medical_record_service.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord,String> {
    List<MedicalRecord> getMedicalRecordByPatientId(String patientId);

    Optional<MedicalRecord> findMedicalRecordByAppointmentId(String appointmentId);

    boolean existsByAppointmentId(String appointmentId);
}
