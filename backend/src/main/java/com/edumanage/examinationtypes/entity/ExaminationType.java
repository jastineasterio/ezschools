package com.edumanage.examinationtypes.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "examination_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "name"})
})
@Getter
@Setter
public class ExaminationType extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
