package com.hehe.doctor_service.service;

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

    public DoctorResponse create(CreationDoctorRequest request){
        Doctor doctor = doctorMapper.toEntity(request);
        return doctorMapper.toResponse(doctorRepository.save(doctor));
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
        doctorMapper.updateEntity(doctor,request);
        doctorRepository.save(doctor);
        return doctorMapper.toResponse(doctor);
    }

    public void delete(String id){
        doctorRepository.deleteById(id);
    }

}
