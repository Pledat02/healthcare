package com.hehe.patient_service.repository;

import com.hehe.patient_service.entity.Gender;
import com.hehe.patient_service.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient,String> {
    @Query("""
            SELECT p FROM patients p
            WHERE (:gender IS NULL OR p.gender = :gender)
              AND (:query = ''
                OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR p.phone LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Patient> search(
            @Param("query") String query,
            @Param("gender") Gender gender,
            Pageable pageable);

    // Tìm bệnh nhân theo keycloakId — cần ở Giai đoạn 4 (phân quyền "của mình")
    Optional<Patient> findByKeycloakId(String keycloakId);

    // Kiểm tra SĐT đã tồn tại chưa — dùng khi muốn chặn trùng phone
    boolean existsByPhone(String phone);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
    boolean existsByKeycloakId(String keycloakId);
}
