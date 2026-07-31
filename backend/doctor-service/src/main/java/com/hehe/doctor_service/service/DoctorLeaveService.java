package com.hehe.doctor_service.service;

import com.hehe.doctor_service.dto.request.CreateLeaveRequest;
import com.hehe.doctor_service.dto.response.DoctorLeaveResponse;
import com.hehe.doctor_service.entity.Doctor;
import com.hehe.doctor_service.entity.DoctorLeave;
import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import com.hehe.doctor_service.repository.DoctorLeaveRepository;
import com.hehe.doctor_service.repository.DoctorRepository;
import com.hehe.doctor_service.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorLeaveService {

    DoctorLeaveRepository leaveRepository;
    DoctorRepository doctorRepository;

    // Bac si dang nhap -> ho so bac si (qua keycloakId)
    private Doctor currentDoctor() {
        String keycloakId = SecurityUtils.getKeyCloakId();
        return doctorRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
    }

    public DoctorLeaveResponse addMyLeave(CreateLeaveRequest request) {
        Doctor me = currentDoctor();
        if (request.getLeaveDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.LEAVE_DATE_IN_PAST);
        }
        if (leaveRepository.existsByDoctorIdAndLeaveDate(me.getId(), request.getLeaveDate())) {
            throw new AppException(ErrorCode.LEAVE_ALREADY_EXISTS);
        }
        DoctorLeave leave = new DoctorLeave();
        leave.setDoctorId(me.getId());
        leave.setLeaveDate(request.getLeaveDate());
        leave.setReason(request.getReason());
        return toResponse(leaveRepository.save(leave));
    }

    public List<DoctorLeaveResponse> getMyLeaves() {
        Doctor me = currentDoctor();
        return getUpcoming(me.getId());
    }

    public void deleteMyLeave(String leaveId) {
        Doctor me = currentDoctor();
        DoctorLeave leave = leaveRepository.findByIdAndDoctorId(leaveId, me.getId())
                .orElseThrow(() -> new AppException(ErrorCode.LEAVE_NOT_FOUND));
        leaveRepository.delete(leave);
    }

    // Ngay nghi sap toi cua 1 bac si - cho FE slot picker + appointment-service kiem tra
    public List<DoctorLeaveResponse> getUpcoming(String doctorId) {
        return leaveRepository
                .findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDate(doctorId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    private DoctorLeaveResponse toResponse(DoctorLeave l) {
        return DoctorLeaveResponse.builder()
                .id(l.getId()).leaveDate(l.getLeaveDate()).reason(l.getReason()).build();
    }
}
