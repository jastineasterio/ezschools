package com.edumanage.subjects.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.departments.entity.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "code"}),
        @UniqueConstraint(columnNames = {"school_id", "name"})
})
@Getter
@Setter
public class Subject extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;
}
