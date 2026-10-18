package com.edumanage.schools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchoolRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String region;
    private String district;
    private boolean isActive;
}
