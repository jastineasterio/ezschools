package com.edumanage.parents.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private String occupation;
    private String address;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
