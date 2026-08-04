package com.hehe.doctor_service.repository;

import com.hehe.doctor_service.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor,String> {
    Optional<Doctor> findByKeycloakId(String id);
    Optional<Doctor> findByEmail(String email);
}
