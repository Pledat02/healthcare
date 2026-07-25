package com.hehe.medical_record_service.service;

import com.hehe.medical_record_service.client.AppointmentClient;
import com.hehe.medical_record_service.client.DoctorClient;
import com.hehe.medical_record_service.client.PatientClient;
import com.hehe.medical_record_service.dto.request.CreationMedicalRecordRequest;
import com.hehe.medical_record_service.dto.response.AppointmentDto;
import com.hehe.medical_record_service.dto.response.DoctorDto;
import com.hehe.medical_record_service.dto.response.MedicalRecordResponse;
import com.hehe.medical_record_service.dto.response.PatientDto;
import com.hehe.medical_record_service.entity.MedicalRecord;
import com.hehe.medical_record_service.exception.AppException;
import com.hehe.medical_record_service.exception.ErrorCode;
import com.hehe.medical_record_service.mapper.MedicalRecordMapper;
import com.hehe.medical_record_service.repository.MedicalRecordRepository;
import com.hehe.medical_record_service.utils.AppointmentStatus;
import com.hehe.medical_record_service.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class MedicalRecordService {
    MedicalRecordRepository medicalRecordRepository;
    MedicalRecordMapper medicalRecordMapper;
    AppointmentClient appointmentClient;
    DoctorClient doctorClient;
    PatientClient  patientClient;

    //api/medical-records
    public MedicalRecordResponse create(CreationMedicalRecordRequest request){
        AppointmentDto appt = appointmentClient.getAppointment(request.getAppointmentId());

        // US-10: đúng bác sĩ khám buổi đó
        if (!appt.getDoctorId().equals(doctorClient.getMe().getId())) {
            throw new AppException(ErrorCode.NOT_THE_TREATING_DOCTOR);
        }
        // BR-05: lịch phải đã khám xong
        if (appt.getStatus() != AppointmentStatus.COMPLETED) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_COMPLETED);
        }
        if (medicalRecordRepository.existsByAppointmentId(request.getAppointmentId())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_EXISTS);
        }
            return medicalRecordMapper.toResponse(
                    medicalRecordRepository.save(
                            medicalRecordMapper.toEntity(request)));
    }
    //api/medical-records/me
    public List<MedicalRecordResponse> getMyHistoryPatientMedicalRecord(){
        PatientDto currentLoginPatient = patientClient.getMe();
        return medicalRecordRepository.getMedicalRecordByPatientId(currentLoginPatient.getId())
                .stream().map(medicalRecordMapper::toResponse).toList();
    }
    //api/medical-records/patients/{patientId}
    public List<MedicalRecordResponse> getHistoryPatientMedicalRecord( String patientId){
        if(!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("DOCTOR"))
            throw new AppException(ErrorCode.FORBIDDEN);
        return medicalRecordRepository.getMedicalRecordByPatientId(patientId)
                .stream().map(medicalRecordMapper::toResponse).toList();
    }

    //api/medical-records/patients/{patientId}
    public MedicalRecordResponse getOne(String id){
        MedicalRecord mr =medicalRecordRepository.findById(id)
                .orElseThrow(()-> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (!SecurityUtils.hasRole("ADMIN") && isNotOwnerPatient(mr.getPatientId()) && isNotTreatingDoctor(mr.getAppointmentId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return medicalRecordMapper.toResponse(mr);
    }
    //api/medical-records/appointments/{appointmentId}
    public MedicalRecordResponse getByAppointmentId(String appointmentId){
        MedicalRecord mr =medicalRecordRepository.findMedicalRecordByAppointmentId(appointmentId)
                .orElseThrow(()-> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!SecurityUtils.hasRole("ADMIN") && isNotOwnerPatient(mr.getPatientId()) && isNotTreatingDoctor(appointmentId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
            return medicalRecordMapper.toResponse(mr);
    }

    private boolean isNotTreatingDoctor(String appointmentId){
        String doctorId = appointmentClient.getAppointment(appointmentId).getDoctorId();
        return !doctorId.equals(doctorClient.getMe().getId());
    }
    private boolean isNotOwnerPatient(String patientId){
        PatientDto patientDto = patientClient.getPatient(patientId);
        PatientDto currentLoginPatient = patientClient.getMe();
        return !patientDto.equals(currentLoginPatient);
    }

}
