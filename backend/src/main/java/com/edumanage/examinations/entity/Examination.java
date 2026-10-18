package com.edumanage.examinations.entity;

import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.examinationtypes.entity.ExaminationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "examinations")
@Getter
@Setter
public class Examination extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", insertable = false, updatable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", insertable = false, updatable = false)
    private ExaminationType type;
}
