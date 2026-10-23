package com.edumanage.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchoolUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    private String phone;

    @NotBlank(message = "Role is required")
    private String role;

    @Size(max = 100)
    private String admissionNumber;

    @Size(max = 20)
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDate registrationDate;

    @Size(max = 50)
    private String studentType;

    private String address;

    @Size(max = 255)
    private String emergencyContactName;

    @Size(max = 30)
    private String emergencyContactPhone;

    @Size(max = 20)
    private String employmentStatus;

    private String qualification;
    private Integer experienceYears;

    @Size(max = 100)
    private String occupation;
}
