package com.edumanage.teacherprofiles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeacherProfileRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String employeeNumber;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Employment status is required")
    private String employmentStatus;

    @NotBlank(message = "Qualification is required")
    private String qualification;

    @NotNull(message = "Experience years is required")
    private Integer experienceYears;

    private String photoUrl;
}
