package com.edumanage.audit.repository;

import com.edumanage.audit.entity.AuditLog;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findBySchoolId(Long schoolId, Pageable pageable);
    Page<AuditLog> findByActorUserId(Long actorUserId, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end")
    Page<AuditLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (CAST(:search AS string) IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND a.schoolId = :schoolId")
    Page<AuditLog> findFilteredBySchoolId(@Param("schoolId") Long schoolId,
                                         @Param("search") String search,
                                         @Param("action") String action,
                                         @Param("entityType") String entityType,
                                         Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (CAST(:search AS string) IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType)")
    Page<AuditLog> findFiltered(@Param("search") String search,
                               @Param("action") String action,
                               @Param("entityType") String entityType,
                               Pageable pageable);
}
