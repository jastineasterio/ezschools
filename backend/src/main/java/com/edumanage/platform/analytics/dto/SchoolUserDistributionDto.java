package com.edumanage.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolUserDistributionDto {
    private Long schoolId;
    private String schoolName;
    private long totalUsers;
    private long managers;
    private long teachers;
    private long students;
    private long parents;
    private long accountants;
    private long hods;
    private long librarians;
    private long wardens;
    private long transportOfficers;
    private long otherRoles;
    private long activeUsers;
    private long inactiveUsers;
}
