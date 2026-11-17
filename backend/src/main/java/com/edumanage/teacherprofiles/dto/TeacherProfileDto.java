package com.edumanage.teacherprofiles.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileDto {
    private Long id;
    private String uuid;
    private Long userId;
    private Long schoolId;
    private String schoolName;
    private String schoolCode;
    private String role;
    private String employeeNumber;
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;
    @Size(max = 100)
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;
    @Size(max = 30)
    private String phone;
    @Size(max = 20)
    private String gender;
    private LocalDate dateOfBirth;
    private String employmentStatus;
    private boolean isActive;
    private LocalDateTime lastLogin;
    private String qualification;
    private Integer experienceYears;
    @Size(max = 500)
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
