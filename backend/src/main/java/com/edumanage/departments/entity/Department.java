package com.edumanage.departments.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "name"}),
        @UniqueConstraint(columnNames = {"school_id", "code"})
})
@Getter
@Setter
public class Department extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "head_of_department_id")
    private Long headOfDepartmentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_of_department_id", insertable = false, updatable = false)
    private User headOfDepartment;
}
