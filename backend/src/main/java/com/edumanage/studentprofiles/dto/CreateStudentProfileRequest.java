package com.edumanage.studentprofiles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentProfileRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String admissionNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String gender;

    private LocalDate dateOfBirth;

    private LocalDate registrationDate;

    private String phone;

    private String email;

    private String address;

    private String photoUrl;

    private String studentType;

    private String status;

    private String emergencyContactName;

    private String emergencyContactPhone;
}
