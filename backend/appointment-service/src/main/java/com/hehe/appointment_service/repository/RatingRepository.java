package com.hehe.appointment_service.repository;

import com.hehe.appointment_service.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, String> {

    boolean existsByAppointmentId(String appointmentId);

    // Danh dau lich nao da danh gia (batch)
    List<Rating> findByAppointmentIdIn(Collection<String> appointmentIds);

    // Danh sach danh gia cua 1 bac si, moi nhat truoc
    List<Rating> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
}
