package com.edumanage.users.repository;

import com.edumanage.users.entity.User;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmail(String email);

    Optional<User> findByUuid(UUID uuid);

    boolean existsByEmail(String email);

    Page<User> findBySchoolId(Long schoolId, Pageable pageable);

    Page<User> findBySchoolIdIsNull(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.schoolId IS NULL")
    Page<User> findSuperAdmins(Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    Page<User> findAllWithRolesBy(Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE UPPER(r.name) = 'MANAGER' AND u.deletedAt IS NULL ORDER BY u.id")
    List<User> findManagers();

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true AND u.deletedAt IS NULL")
    List<User> findActiveUsersByRoleName(@Param("roleName") String roleName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.schoolId = :schoolId "
            + "AND u.isActive = true AND u.deletedAt IS NULL AND UPPER(r.name) IN :roleNames")
    List<User> findActiveBySchoolAndRoleNames(@Param("schoolId") Long schoolId,
                                               @Param("roleNames") List<String> roleNames);
}
