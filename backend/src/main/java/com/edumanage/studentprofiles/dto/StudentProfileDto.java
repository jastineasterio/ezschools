package com.edumanage.studentprofiles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDto {
    private Long id;
    private String uuid;
    private Long userId;
    private Long schoolId;
    private String admissionNumber;
    // One-time initial password returned only on creation
    private String initialPassword;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
