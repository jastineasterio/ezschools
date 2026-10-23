package com.edumanage.certificates.repository;

import com.edumanage.certificates.entity.StudentCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentCertificateRepository extends JpaRepository<StudentCertificate, Long> {
    List<StudentCertificate> findBySchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long schoolId);
    List<StudentCertificate> findByStudentIdAndSchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long studentId, Long schoolId);
    Optional<StudentCertificate> findByIdAndSchoolIdAndDeletedAtIsNull(Long id, Long schoolId);
    Optional<StudentCertificate> findByVerificationCodeAndDeletedAtIsNull(String verificationCode);
    @Query("""
            select case when count(c) > 0 then true else false end from StudentCertificate c
            where c.schoolId = :schoolId and c.studentId = :studentId
              and c.academicYearId = :yearId and c.classId = :classId and c.awardType = :awardType
              and c.deletedAt is null and c.status in ('GENERATED', 'RELEASED')
              and ((:streamId is null and c.streamId is null) or c.streamId = :streamId)
              and ((:subjectId is null and c.subjectId is null) or c.subjectId = :subjectId)
            """)
    boolean existsActiveInScope(@Param("schoolId") Long schoolId, @Param("studentId") Long studentId,
                                @Param("yearId") Long yearId, @Param("classId") Long classId,
                                @Param("streamId") Long streamId, @Param("subjectId") Long subjectId,
                                @Param("awardType") String awardType);

    @Query("""
            select coalesce(max(c.version), 0) + 1 from StudentCertificate c
            where c.schoolId = :schoolId and c.studentId = :studentId
              and c.academicYearId = :yearId and c.classId = :classId and c.awardType = :awardType
              and ((:streamId is null and c.streamId is null) or c.streamId = :streamId)
              and ((:subjectId is null and c.subjectId is null) or c.subjectId = :subjectId)
            """)
    Integer nextVersion(@Param("schoolId") Long schoolId, @Param("studentId") Long studentId,
                        @Param("yearId") Long yearId, @Param("classId") Long classId,
                        @Param("streamId") Long streamId, @Param("subjectId") Long subjectId,
                        @Param("awardType") String awardType);
}
