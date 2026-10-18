package com.edumanage.examinationsubjects.repository;

import com.edumanage.examinationsubjects.entity.ExaminationSubject;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationSubjectRepository extends JpaRepository<ExaminationSubject, Long> {
    Page<ExaminationSubject> findByExaminationId(Long examinationId, Pageable pageable);
    List<ExaminationSubject> findByExaminationId(Long examinationId);
    Optional<ExaminationSubject> findByExaminationIdAndSubjectIdAndClassIdAndStreamId(Long examinationId, Long subjectId, Long classId, Long streamId);
}
