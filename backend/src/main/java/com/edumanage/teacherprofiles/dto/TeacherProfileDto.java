package com.edumanage.teacherprofiles.dto;

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
public class TeacherProfileDto {
    private Long id;
    private String uuid;
    private Long userId;
    private Long schoolId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String gender;
    private LocalDate dateOfBirth;
    private String employmentStatus;
    private String qualification;
    private Integer experienceYears;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
