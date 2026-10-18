package com.edumanage.finance.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_categories")
@Getter
@Setter
public class FinancialCategory extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "category_type", nullable = false, length = 20)
    private String categoryType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
