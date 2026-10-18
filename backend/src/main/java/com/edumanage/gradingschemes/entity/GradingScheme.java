package com.edumanage.gradingschemes.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "grading_schemes")
@Getter
@Setter
public class GradingScheme extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
