package com.edumanage.schools.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchoolRequest {
    @NotBlank(message = "School name is required")
    private String name;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    private String address;

    private String region;

    private String district;
}
