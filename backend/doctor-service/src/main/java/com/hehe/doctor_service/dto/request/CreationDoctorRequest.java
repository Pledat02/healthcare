package com.hehe.doctor_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Time;
import java.time.LocalTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreationDoctorRequest {
    // US-03: admin dat luon tai khoan dang nhap cho bac si (tao ben Keycloak)
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$",
            message = "Tên đăng nhập 3-30 ký tự, chỉ gồm chữ, số và . _ -")
    String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    String password;

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
