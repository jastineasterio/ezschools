package com.edumanage.users.repository;

import com.edumanage.users.entity.User;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    interface SchoolUserCount {
        Long getSchoolId();
        Long getUserCount();
    }

    @Query("SELECT u.schoolId AS schoolId, COUNT(u.id) AS userCount FROM User u "
            + "WHERE u.schoolId IN :schoolIds AND u.deletedAt IS NULL "
            + "AND NOT EXISTS (SELECT platformRole FROM User platformUser JOIN platformUser.roles platformRole "
            + "WHERE platformUser = u AND UPPER(platformRole.name) = 'SUPER_ADMIN') "
            + "GROUP BY u.schoolId")
    List<SchoolUserCount> countSchoolUsersBySchoolIds(@Param("schoolIds") List<Long> schoolIds);

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmail(String email);

    Optional<User> findByUuid(UUID uuid);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    Page<User> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<User> findByIdAndSchoolIdAndDeletedAtIsNull(Long id, Long schoolId);

    Page<User> findBySchoolIdIsNull(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.schoolId IS NULL")
    Page<User> findSuperAdmins(Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    Page<User> findAllWithRolesBy(Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE u.schoolId = :schoolId AND u.deletedAt IS NULL "
            + "AND NOT EXISTS (SELECT superRole FROM User platformUser JOIN platformUser.roles superRole "
            + "WHERE platformUser = u AND UPPER(superRole.name) = 'SUPER_ADMIN') "
            + "AND (:active IS NULL OR u.isActive = :active) "
                    + "AND (CAST(:search AS string) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) "
                    + "LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
                    + "AND (CAST(:role AS string) IS NULL OR EXISTS (SELECT matchedRole FROM User roleUser JOIN roleUser.roles matchedRole "
                    + "WHERE roleUser = u AND UPPER(matchedRole.name) = UPPER(CAST(:role AS string))))",
            countQuery = "SELECT COUNT(u) FROM User u WHERE u.schoolId = :schoolId AND u.deletedAt IS NULL "
                    + "AND NOT EXISTS (SELECT superRole FROM User platformUser JOIN platformUser.roles superRole "
                    + "WHERE platformUser = u AND UPPER(superRole.name) = 'SUPER_ADMIN') "
                    + "AND (:active IS NULL OR u.isActive = :active) "
                            + "AND (CAST(:search AS string) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
                    + "OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) "
                            + "LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
                            + "AND (CAST(:role AS string) IS NULL OR EXISTS (SELECT matchedRole FROM User roleUser JOIN roleUser.roles matchedRole "
                            + "WHERE roleUser = u AND UPPER(matchedRole.name) = UPPER(CAST(:role AS string))))")
    Page<User> findSchoolUsers(@Param("schoolId") Long schoolId,
                               @Param("search") String search,
                               @Param("role") String role,
                               @Param("active") Boolean active,
                               Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE UPPER(r.name) = 'MANAGER' AND u.deletedAt IS NULL ORDER BY u.id")
    List<User> findManagers();

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true AND u.deletedAt IS NULL")
    List<User> findActiveUsersByRoleName(@Param("roleName") String roleName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.schoolId = :schoolId "
            + "AND u.isActive = true AND u.deletedAt IS NULL AND UPPER(r.name) IN :roleNames")
    List<User> findActiveBySchoolAndRoleNames(@Param("schoolId") Long schoolId,
                                               @Param("roleNames") List<String> roleNames);
}
