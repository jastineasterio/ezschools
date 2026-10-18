package com.edumanage.teacherprofiles.repository;

import com.edumanage.teacherprofiles.entity.TeacherProfile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByUserId(Long userId);
    Optional<TeacherProfile> findBySchoolIdAndUserId(Long schoolId, Long userId);
    Page<TeacherProfile> findBySchoolId(Long schoolId, Pageable pageable);
    Page<TeacherProfile> findBySchoolIdAndDeletedAtIsNull(Long schoolId, Pageable pageable);
    long countBySchoolIdAndDeletedAtIsNull(Long schoolId);
}
