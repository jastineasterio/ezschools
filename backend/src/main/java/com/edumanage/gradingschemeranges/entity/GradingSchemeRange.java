package com.edumanage.gradingschemeranges.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.gradingschemes.entity.GradingScheme;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "grading_scheme_ranges")
@Getter
@Setter
public class GradingSchemeRange extends SoftDeletableEntity {

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "grade", nullable = false, length = 10)
    private String grade;

    @Column(name = "min_mark", nullable = false, precision = 10, scale = 2)
    private BigDecimal minMark;

    @Column(name = "max_mark", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxMark;

    @Column(name = "grade_point", precision = 10, scale = 2)
    private BigDecimal gradePoint;

    @Column(name = "remark", length = 100)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", insertable = false, updatable = false)
    private GradingScheme scheme;
}
