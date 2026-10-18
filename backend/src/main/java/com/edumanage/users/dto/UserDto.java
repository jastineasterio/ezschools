package com.edumanage.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String uuid;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Long schoolId;
    @JsonProperty("isActive")
    private boolean isActive;
    private String schoolName;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private List<String> roles;
}
