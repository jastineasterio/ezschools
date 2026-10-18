package com.edumanage.examinationreports.repository;

import com.edumanage.examinationreports.entity.ExaminationReport;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationReportRepository extends JpaRepository<ExaminationReport, Long> {
    Page<ExaminationReport> findBySchoolId(Long schoolId, Pageable pageable);
    Page<ExaminationReport> findByStudentIdAndSchoolId(Long studentId, Long schoolId, Pageable pageable);
    Optional<ExaminationReport> findByStudentIdAndExaminationId(Long studentId, Long examinationId);
}
