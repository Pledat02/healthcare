package com.hehe.doctor_service.service;

import com.hehe.doctor_service.client.KeycloakAdminClient;
import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.request.UpdateDoctorRequest;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.entity.Doctor;
import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import com.hehe.doctor_service.mapper.DoctorMapper;
import com.hehe.doctor_service.repository.DoctorRepository;
import com.hehe.doctor_service.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class DoctorService {
    DoctorMapper doctorMapper ;
    DoctorRepository doctorRepository;
    KeycloakAdminClient keycloakAdminClient;

    // US-03: admin them bac si -> tao luon tai khoan Keycloak (role DOCTOR)
    // va noi voi ho so qua keycloakId, de bac si dang nhap duoc ngay.
    public DoctorResponse create(CreationDoctorRequest request){
        String keycloakId = keycloakAdminClient.createDoctorUser(
                request.getUsername(), request.getPassword(),
                request.getFullName(), request.getEmail());
        try {
            Doctor doctor = doctorMapper.toEntity(request);
            doctor.setKeycloakId(keycloakId);
            return doctorMapper.toResponse(doctorRepository.save(doctor));
        } catch (RuntimeException e) {
            // Luu DB that bai -> go bo tai khoan vua tao, tranh user mo coi
            keycloakAdminClient.deleteUser(keycloakId);
            throw e;
        }
    }
    public DoctorResponse getOne(String id){

        Doctor doctor = doctorRepository.findById(id).orElseThrow(
                ()-> new AppException(ErrorCode.DOCTOR_NOT_FOUND)
        );
//        if (!SecurityUtils.isAccessed(doctor)) throw new AppException(ErrorCode.FORBIDDEN);
        return doctorMapper.toResponse(doctor);
    }

    public List<DoctorResponse> getAll(){
        // check admin

        return doctorRepository.findAll().stream().
        map(doctorMapper::toResponse).toList();
    }

    public DoctorResponse update(String id, UpdateDoctorRequest request){
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(
                        () -> new AppException(ErrorCode.DOCTOR_NOT_FOUND)
                );
//        if (!SecurityUtils.isAccessed(doctor)) throw new AppException(ErrorCode.FORBIDDEN);
         doctor = doctorMapper.updateEntity(doctor,request);
        doctorRepository.save(doctor);
        return doctorMapper.toResponse(doctor);
    }
    public DoctorResponse getMe(){
        String idKeyCloak = SecurityUtils.getKeyCloakId();
        return doctorMapper.toResponse(doctorRepository.findByKeycloakId(idKeyCloak).orElseThrow(() ->
                new AppException(ErrorCode.DOCTOR_NOT_FOUND)));
    }

    public void delete(String id){
        // Xoa ca tai khoan Keycloak de khong con user mo coi khong ai dung
        doctorRepository.findById(id)
                .ifPresent(d -> keycloakAdminClient.deleteUser(d.getKeycloakId()));
        doctorRepository.deleteById(id);
    }

}
