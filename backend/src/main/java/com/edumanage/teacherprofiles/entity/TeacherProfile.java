package com.edumanage.teacherprofiles.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "teacher_profiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
@Getter
@Setter
public class TeacherProfile extends SoftDeletableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "employee_number", length = 100)
    private String employeeNumber;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "employment_status", nullable = false, length = 20)
    private String employmentStatus = "ACTIVE";

    @Column(name = "qualification", columnDefinition = "TEXT")
    private String qualification;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
