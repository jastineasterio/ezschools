package com.edumanage.academicyears.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "academic_years", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"school_id", "name"})
})
@Getter
@Setter
public class AcademicYear extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "UPCOMING";

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = false;
}
