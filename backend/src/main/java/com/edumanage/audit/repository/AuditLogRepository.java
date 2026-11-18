package com.edumanage.audit.repository;

import com.edumanage.audit.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findBySchoolId(Long schoolId, Pageable pageable);
    Page<AuditLog> findByActorUserId(Long actorUserId, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:start IS NULL OR a.createdAt >= :start) " +
            "AND (:endExclusive IS NULL OR a.createdAt < :endExclusive) " +
            "AND (:result IS NULL OR a.result = :result) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:schoolId IS NULL OR a.schoolId = :schoolId) " +
            "AND (:search IS NULL OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.actorName, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AuditLog> findFiltered(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "a.schoolId = :schoolId " +
            "AND (:start IS NULL OR a.createdAt >= :start) " +
            "AND (:endExclusive IS NULL OR a.createdAt < :endExclusive) " +
            "AND (:result IS NULL OR a.result = :result) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:search IS NULL OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.actorName, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AuditLog> findFilteredBySchoolId(
            @Param("schoolId") Long schoolId,
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE " +
            "(:start IS NULL OR a.createdAt >= :start) " +
            "AND (:endExclusive IS NULL OR a.createdAt < :endExclusive) " +
            "AND (:result IS NULL OR a.result = :result) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:schoolId IS NULL OR a.schoolId = :schoolId) " +
            "AND (:search IS NULL OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.actorName, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    long countFiltered(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query("SELECT COUNT(DISTINCT a.actorUserId) FROM AuditLog a WHERE " +
            "(:start IS NULL OR a.createdAt >= :start) " +
            "AND (:endExclusive IS NULL OR a.createdAt < :endExclusive) " +
            "AND (:result IS NULL OR a.result = :result) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:schoolId IS NULL OR a.schoolId = :schoolId) " +
            "AND (:search IS NULL OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.actorName, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND a.actorUserId IS NOT NULL")
    long countDistinctActorsFiltered(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE " +
            "(:start IS NULL OR a.createdAt >= :start) " +
            "AND (:endExclusive IS NULL OR a.createdAt < :endExclusive) " +
            "AND (:result IS NULL OR a.result = :result) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:schoolId IS NULL OR a.schoolId = :schoolId) " +
            "AND (:search IS NULL OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.actorName, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND a.result = 'FAILURE'")
    long countFailuresFiltered(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE " +
            "(:start IS NULL OR a.createdAt >= :start) " +
            "AND (:endExclusive IS NULL OR a.createdAt < :endExclusive) " +
            "AND (:result IS NULL OR a.result = :result) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:schoolId IS NULL OR a.schoolId = :schoolId) " +
            "AND (:search IS NULL OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(a.actorName, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND a.result = 'SUCCESS'")
    long countSuccessesFiltered(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query(value = """
            SELECT to_char(d, 'YYYY-MM-DD'), COALESCE(cnt, 0)
            FROM generate_series(:start::date, (:endExclusive - interval '1 day')::date, interval '1 day') AS d(day)
            LEFT JOIN (
                SELECT DATE(created_at) AS day, COUNT(*) AS cnt
                FROM audit_logs
                WHERE created_at >= :start AND created_at < :endExclusive
                  AND (:result IS NULL OR result = :result)
                  AND (:actorUserId IS NULL OR actor_user_id = :actorUserId)
                  AND (:action IS NULL OR action = :action)
                  AND (:entityType IS NULL OR entity_type = :entityType)
                  AND (:schoolId IS NULL OR school_id = :schoolId)
                  AND (:search IS NULL OR LOWER(action) LIKE LOWER('%' || :search || '%')
                       OR LOWER(COALESCE(description, '')) LIKE LOWER('%' || :search || '%')
                       OR LOWER(COALESCE(entity_type, '')) LIKE LOWER('%' || :search || '%')
                       OR LOWER(COALESCE(actor_name, '')) LIKE LOWER('%' || :search || '%'))
                GROUP BY DATE(created_at)
            ) a ON a.day = d.day
            ORDER BY d.day
            """, nativeQuery = true)
    List<Object[]> dailyTrend(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query(value = """
            SELECT COALESCE(action, 'Unknown'), COUNT(*)
            FROM audit_logs
            WHERE created_at >= :start AND created_at < :endExclusive
              AND (:result IS NULL OR result = :result)
              AND (:actorUserId IS NULL OR actor_user_id = :actorUserId)
              AND (:action IS NULL OR action = :action)
              AND (:entityType IS NULL OR entity_type = :entityType)
              AND (:schoolId IS NULL OR school_id = :schoolId)
              AND (:search IS NULL OR LOWER(action) LIKE LOWER('%' || :search || '%')
                   OR LOWER(COALESCE(description, '')) LIKE LOWER('%' || :search || '%')
                   OR LOWER(COALESCE(entity_type, '')) LIKE LOWER('%' || :search || '%')
                   OR LOWER(COALESCE(actor_name, '')) LIKE LOWER('%' || :search || '%'))
            GROUP BY action
            ORDER BY COUNT(*) DESC, action
            """, nativeQuery = true)
    List<Object[]> actionDistribution(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query(value = """
            SELECT COALESCE(result, 'Unknown'), COUNT(*)
            FROM audit_logs
            WHERE created_at >= :start AND created_at < :endExclusive
              AND (:result IS NULL OR result = :result)
              AND (:actorUserId IS NULL OR actor_user_id = :actorUserId)
              AND (:action IS NULL OR action = :action)
              AND (:entityType IS NULL OR entity_type = :entityType)
              AND (:schoolId IS NULL OR school_id = :schoolId)
              AND (:search IS NULL OR LOWER(action) LIKE LOWER('%' || :search || '%')
                   OR LOWER(COALESCE(description, '')) LIKE LOWER('%' || :search || '%')
                   OR LOWER(COALESCE(entity_type, '')) LIKE LOWER('%' || :search || '%')
                   OR LOWER(COALESCE(actor_name, '')) LIKE LOWER('%' || :search || '%'))
            GROUP BY result
            ORDER BY result
            """, nativeQuery = true)
    List<Object[]> resultDistribution(
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("result") String result,
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("schoolId") Long schoolId,
            @Param("search") String search);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt < :cutoff")
    long countOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM audit_logs WHERE created_at < :cutoff LIMIT :batch", nativeQuery = true)
    int deleteOlderThanBatch(@Param("cutoff") LocalDateTime cutoff, @Param("batch") Integer batch);

    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC, a.id DESC")
    List<AuditLog> findLatest(Pageable pageable);
}
