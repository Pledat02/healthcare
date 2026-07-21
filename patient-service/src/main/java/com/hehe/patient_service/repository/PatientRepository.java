package com.hehe.patient_service.repository;

import com.hehe.patient_service.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient,String> {
    // Tìm bệnh nhân theo keycloakId — cần ở Giai đoạn 4 (phân quyền "của mình")
    Optional<Patient> findByKeycloakId(String keycloakId);

    // Kiểm tra SĐT đã tồn tại chưa — dùng khi muốn chặn trùng phone
    boolean existsByPhone(String phone);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

}
