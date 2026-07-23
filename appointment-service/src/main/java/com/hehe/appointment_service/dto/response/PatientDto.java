package com.hehe.appointment_service.dto.response;

import com.hehe.appointment_service.utils.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PatientDto {

    String id;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @Past(message = "Ngày sinh phải trong quá khứ")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String address;
}
