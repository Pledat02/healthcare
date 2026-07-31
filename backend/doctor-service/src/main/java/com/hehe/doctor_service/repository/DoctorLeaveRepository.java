package com.hehe.doctor_service.repository;

import com.hehe.doctor_service.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, String> {

    // Ngay nghi sap toi cua 1 bac si (>= from), sap xep tang dan
    List<DoctorLeave> findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDate(String doctorId, LocalDate from);

    boolean existsByDoctorIdAndLeaveDate(String doctorId, LocalDate leaveDate);

    Optional<DoctorLeave> findByIdAndDoctorId(String id, String doctorId);
}
