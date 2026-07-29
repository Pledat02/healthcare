package com.hehe.doctor_service.dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorResponse implements Serializable {   // Serializable de cache vao Redis (JDK serialization)

    String id;

    @NotBlank(message = "Tên bác sĩ không được để trống")
    String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
    String phone;

    @Email(message = "Vui lòng nhập đúng định dạng email")
    String email;
    @NotBlank(message = "Chuyên khoa không được để trống")
    String specialization;

    @NotNull(message = "Giờ bắt đầu làm không được để trống")
    LocalTime workStartTime;

    @NotNull(message = "Giờ kết thúc làm không được để trống")
    LocalTime workEndTime;
}
