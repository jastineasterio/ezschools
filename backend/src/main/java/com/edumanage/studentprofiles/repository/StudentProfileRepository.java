package com.edumanage.studentprofiles.repository;

import com.edumanage.studentprofiles.entity.StudentProfile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Page<StudentProfile> findBySchoolId(Long schoolId, Pageable pageable);
    Page<StudentProfile> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    Optional<StudentProfile> findBySchoolIdAndAdmissionNumber(Long schoolId, String admissionNumber);
    Optional<StudentProfile> findBySchoolIdAndId(Long schoolId, Long id);
    Optional<StudentProfile> findByUserId(Long userId);
    boolean existsBySchoolIdAndAdmissionNumber(Long schoolId, String admissionNumber);
    long countBySchoolIdAndDeletedAtIsNull(Long schoolId);
}
