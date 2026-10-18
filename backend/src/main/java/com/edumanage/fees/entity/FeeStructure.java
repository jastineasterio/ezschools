package com.edumanage.fees.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fee_structures")
@Getter
@Setter
public class FeeStructure extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "academic_year_id")
    private Long academicYearId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "fee_type", nullable = false, length = 50)
    private String feeType = "TUITION";

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
