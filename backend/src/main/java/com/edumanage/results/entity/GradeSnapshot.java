package com.edumanage.results.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "result_grade_snapshots")
@Getter
@Setter
public class GradeSnapshot extends SoftDeletableEntity {

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "grading_scheme_id")
    private Long gradingSchemeId;

    @Column(name = "grading_scheme_name", length = 255)
    private String gradingSchemeName;

    @Column(name = "min_mark", precision = 10, scale = 2)
    private java.math.BigDecimal minMark;

    @Column(name = "max_mark", precision = 10, scale = 2)
    private java.math.BigDecimal maxMark;

    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = "grade_ranges", columnDefinition = "JSONB")
    private JsonNode gradeRanges;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static class RangeEntry {
        private String grade;
        private java.math.BigDecimal minMark;
        private java.math.BigDecimal maxMark;
        private java.math.BigDecimal gradePoint;
        private String remark;

        public RangeEntry() {}

        public RangeEntry(String grade, java.math.BigDecimal minMark, java.math.BigDecimal maxMark,
                          java.math.BigDecimal gradePoint, String remark) {
            this.grade = grade;
            this.minMark = minMark;
            this.maxMark = maxMark;
            this.gradePoint = gradePoint;
            this.remark = remark;
        }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public java.math.BigDecimal getMinMark() { return minMark; }
        public void setMinMark(java.math.BigDecimal minMark) { this.minMark = minMark; }
        public java.math.BigDecimal getMaxMark() { return maxMark; }
        public void setMaxMark(java.math.BigDecimal maxMark) { this.maxMark = maxMark; }
        public java.math.BigDecimal getGradePoint() { return gradePoint; }
        public void setGradePoint(java.math.BigDecimal gradePoint) { this.gradePoint = gradePoint; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
