package com.edumanage.platform.analytics.service;

import com.edumanage.platform.analytics.dto.ActiveInactiveDto;
import com.edumanage.platform.analytics.dto.PackageUserDistributionDto;
import com.edumanage.platform.analytics.dto.RoleDistributionDto;
import com.edumanage.platform.analytics.dto.SchoolUserDistributionDto;
import com.edumanage.platform.analytics.dto.TrendDto;
import com.edumanage.platform.analytics.dto.UserOverviewDto;
import com.edumanage.packages.repository.PackageRepository;
import com.edumanage.roles.entity.Role;
import com.edumanage.roles.repository.RoleRepository;
import com.edumanage.schools.repository.SchoolRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformUserAnalyticsService {

    private static final String ROLE_PRIORITY_CASE = "CASE r.name " +
            "WHEN 'SUPER_ADMIN' THEN 1 WHEN 'MANAGER' THEN 2 WHEN 'HOD' THEN 3 " +
            "WHEN 'TEACHER' THEN 4 WHEN 'ACCOUNTANT' THEN 5 WHEN 'LIBRARIAN' THEN 6 " +
            "WHEN 'WARDEN' THEN 7 WHEN 'TRANSPORT_OFFICER' THEN 8 " +
            "WHEN 'PARENT' THEN 9 WHEN 'STUDENT' THEN 10 ELSE 11 END";

    private static final String PRIMARY_ROLE_SUBQUERY =
            "(SELECT r.name FROM user_roles ur " +
                    "JOIN roles r ON r.id = ur.role_id " +
                    "WHERE ur.user_id = u.id " +
                    "ORDER BY " + ROLE_PRIORITY_CASE + " ASC " +
                    "LIMIT 1)";

    private final EntityManager entityManager;
    private final RoleRepository roleRepository;
    private final PackageRepository packageRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public UserOverviewDto getOverview(LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);

        Tuple counts = (Tuple) entityManager.createNativeQuery(
                "SELECT " +
                        "COUNT(*) FILTER (WHERE deleted_at IS NULL) AS total_users, " +
                        "COUNT(*) FILTER (WHERE deleted_at IS NULL AND is_active = true) AS active_users, " +
                        "COUNT(*) FILTER (WHERE deleted_at IS NULL AND is_active = false) AS inactive_users, " +
                        "COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS deleted_users " +
                        "FROM users", Tuple.class)
                .getSingleResult();

        long totalUsers = ((Number) counts.get("total_users")).longValue();
        long activeUsers = ((Number) counts.get("active_users")).longValue();
        long inactiveUsers = ((Number) counts.get("inactive_users")).longValue();
        long deletedUsers = ((Number) counts.get("deleted_users")).longValue();

        long totalSchools = ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM schools").getSingleResult()).longValue();

        double averageUsersPerSchool = totalSchools > 0 ? (double) totalUsers / totalSchools : 0.0;

        Long newUsersPeriod = ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND created_at BETWEEN :start AND :end")
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult()).longValue();

        Long growthPercent = null;
        long periodLengthDays = ChronoUnit.DAYS.between(start, end);
        LocalDateTime previousStart = start.minus(periodLengthDays, ChronoUnit.DAYS);
        Long previousNewUsers = countNewUsersInPeriod(previousStart, start);
        if (previousNewUsers != null && previousNewUsers > 0) {
            growthPercent = Math.round(((double) (newUsersPeriod - previousNewUsers) / previousNewUsers) * 100);
        } else {
            growthPercent = newUsersPeriod > 0 ? null : 0L;
        }

        List<RoleDistributionDto> roleDistribution = getRoleDistribution(start, end, totalUsers);
        List<ActiveInactiveDto> statusDistribution = List.of(
                new ActiveInactiveDto("active", activeUsers, percent(activeUsers, totalUsers)),
                new ActiveInactiveDto("inactive", inactiveUsers, percent(inactiveUsers, totalUsers)),
                new ActiveInactiveDto("deleted", deletedUsers, percent(deletedUsers, totalUsers))
        );

        return new UserOverviewDto(
                totalUsers, activeUsers, inactiveUsers, deletedUsers,
                totalSchools, averageUsersPerSchool, newUsersPeriod,
                growthPercent == null ? null : growthPercent.doubleValue(),
                start, end, roleDistribution, statusDistribution);
    }

    @Transactional(readOnly = true)
    public Page<SchoolUserDistributionDto> getSchoolDistribution(LocalDateTime start, LocalDateTime end,
                                                                 String search, Pageable pageable) {
        validateRange(start, end);

        String searchParam = StringUtils.hasText(search) ? "%" + search.toLowerCase() + "%" : null;

        Long total = ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM schools s " +
                        "WHERE (:search IS NULL OR LOWER(s.name) LIKE :search)")
                .setParameter("search", searchParam)
                .getSingleResult()).longValue();

        if (total == 0) {
            return Page.empty(pageable);
        }

        int offset = (int) Math.min(pageable.getOffset(), Integer.MAX_VALUE);
        int pageSize = pageable.getPageSize();

        String sort = "total_users DESC";
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            sort = (order.getProperty().equalsIgnoreCase("schoolName") ? "s.name" : "total_users")
                    + " " + (order.isAscending() ? "ASC" : "DESC");
        }

        String sql = "WITH user_roles_agg AS (" +
                "  SELECT u.school_id, u.is_active, " + PRIMARY_ROLE_SUBQUERY + " AS primary_role " +
                "  FROM users u WHERE u.deleted_at IS NULL" +
                ")" +
                "SELECT s.id AS school_id, s.name AS school_name, " +
                "  COUNT(ur.school_id) AS total_users, " +
                "  COUNT(CASE WHEN ur.is_active THEN 1 END) AS active_users, " +
                "  COUNT(CASE WHEN NOT ur.is_active THEN 1 END) AS inactive_users, " +
                "  COUNT(CASE WHEN ur.primary_role = 'MANAGER' THEN 1 END) AS managers, " +
                "  COUNT(CASE WHEN ur.primary_role = 'TEACHER' THEN 1 END) AS teachers, " +
                "  COUNT(CASE WHEN ur.primary_role = 'STUDENT' THEN 1 END) AS students, " +
                "  COUNT(CASE WHEN ur.primary_role = 'PARENT' THEN 1 END) AS parents, " +
                "  COUNT(CASE WHEN ur.primary_role = 'ACCOUNTANT' THEN 1 END) AS accountants, " +
                "  COUNT(CASE WHEN ur.primary_role = 'HOD' THEN 1 END) AS hods, " +
                "  COUNT(CASE WHEN ur.primary_role = 'LIBRARIAN' THEN 1 END) AS librarians, " +
                "  COUNT(CASE WHEN ur.primary_role = 'WARDEN' THEN 1 END) AS wardens, " +
                "  COUNT(CASE WHEN ur.primary_role = 'TRANSPORT_OFFICER' THEN 1 END) AS transport_officers, " +
                "  COUNT(CASE WHEN ur.primary_role NOT IN " +
                "    ('MANAGER','TEACHER','STUDENT','PARENT','ACCOUNTANT','HOD','LIBRARIAN','WARDEN','TRANSPORT_OFFICER') " +
                "    AND ur.primary_role IS NOT NULL THEN 1 END) AS other_roles " +
                "FROM schools s " +
                "LEFT JOIN user_roles_agg ur ON ur.school_id = s.id " +
                "WHERE (:search IS NULL OR LOWER(s.name) LIKE :search) " +
                "GROUP BY s.id, s.name " +
                "ORDER BY " + sort + " " +
                "LIMIT :pageSize OFFSET :offset";

        List<Tuple> rows = entityManager.createNativeQuery(sql, Tuple.class)
                .setParameter("search", searchParam)
                .setParameter("pageSize", pageSize)
                .setParameter("offset", offset)
                .getResultList();

        List<SchoolUserDistributionDto> content = new ArrayList<>();
        for (Tuple row : rows) {
            content.add(new SchoolUserDistributionDto(
                    ((Number) row.get("school_id")).longValue(),
                    (String) row.get("school_name"),
                    longValue(row.get("total_users")),
                    longValue(row.get("managers")),
                    longValue(row.get("teachers")),
                    longValue(row.get("students")),
                    longValue(row.get("parents")),
                    longValue(row.get("accountants")),
                    longValue(row.get("hods")),
                    longValue(row.get("librarians")),
                    longValue(row.get("wardens")),
                    longValue(row.get("transport_officers")),
                    longValue(row.get("other_roles")),
                    longValue(row.get("active_users")),
                    longValue(row.get("inactive_users"))
            ));
        }
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public List<PackageUserDistributionDto> getPackageDistribution(LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);

        Map<String, String> packageNames = packageRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> String.valueOf(p.getId()),
                        p -> p.getName(),
                        (a, b) -> a));
        Map<String, String> packageDisplayNames = packageRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> String.valueOf(p.getId()),
                        p -> p.getDisplayName(),
                        (a, b) -> a));

        List<Tuple> rows = entityManager.createNativeQuery(
                "WITH current_sub AS (" +
                        "  SELECT DISTINCT ON (school_id) school_id, package_id " +
                        "  FROM subscriptions WHERE status = 'ACTIVE' " +
                        "  ORDER BY school_id, created_at DESC" +
                        "), school_package AS (" +
                        "  SELECT sch.id AS school_id, cs.package_id " +
                        "  FROM schools sch LEFT JOIN current_sub cs ON cs.school_id = sch.id" +
                        ")" +
                        "SELECT sp.package_id AS package_id, " +
                        "  COUNT(DISTINCT sp.school_id) AS school_count, " +
                        "  COUNT(u.id) AS total_users, " +
                        "  COUNT(CASE WHEN u.is_active THEN 1 END) AS active_users, " +
                        "  COUNT(CASE WHEN NOT u.is_active THEN 1 END) AS inactive_users " +
                        "FROM school_package sp " +
                        "LEFT JOIN users u ON u.school_id = sp.school_id AND u.deleted_at IS NULL " +
                        "GROUP BY sp.package_id " +
                        "ORDER BY total_users DESC", Tuple.class)
                .getResultList();

        Map<Long, Map<String, Long>> roleDistribution = getPackageRoleDistribution();

        List<PackageUserDistributionDto> result = new ArrayList<>();
        for (Tuple row : rows) {
            Object pid = row.get("package_id");
            boolean unassigned = pid == null;
            Long packageId = unassigned ? null : ((Number) pid).longValue();
            String packageName = unassigned ? "Unassigned" : packageNames.getOrDefault(String.valueOf(packageId), "Package " + packageId);
            String displayName = unassigned ? "Unassigned" : packageDisplayNames.getOrDefault(String.valueOf(packageId), packageName);
            long totalUsers = longValue(row.get("total_users"));
            long schoolCount = longValue(row.get("school_count"));
            result.add(new PackageUserDistributionDto(
                    packageId, packageName, displayName, unassigned,
                    schoolCount, totalUsers,
                    schoolCount > 0 ? (double) totalUsers / schoolCount : 0.0,
                    longValue(row.get("active_users")),
                    longValue(row.get("inactive_users")),
                    roleDistribution.getOrDefault(packageId, Map.of())
            ));
        }
        return result;
    }

    private Map<Long, Map<String, Long>> getPackageRoleDistribution() {
        List<Tuple> rows = entityManager.createNativeQuery(
                "WITH current_sub AS (" +
                        "  SELECT DISTINCT ON (school_id) school_id, package_id " +
                        "  FROM subscriptions WHERE status = 'ACTIVE' " +
                        "  ORDER BY school_id, created_at DESC" +
                        "), school_package AS (" +
                        "  SELECT sch.id AS school_id, cs.package_id " +
                        "  FROM schools sch LEFT JOIN current_sub cs ON cs.school_id = sch.id" +
                        ")" +
                        "SELECT sp.package_id AS package_id, ur.primary_role AS role, COUNT(u.id) AS user_count " +
                        "FROM school_package sp " +
                        "JOIN (" +
                        "  SELECT u.id, u.school_id, " + PRIMARY_ROLE_SUBQUERY + " AS primary_role " +
                        "  FROM users u WHERE u.deleted_at IS NULL" +
                        ") ur ON ur.school_id = sp.school_id " +
                        "GROUP BY sp.package_id, ur.primary_role", Tuple.class)
                .getResultList();

        Map<Long, Map<String, Long>> result = new LinkedHashMap<>();
        for (Tuple row : rows) {
            Object pid = row.get("package_id");
            Long packageId = pid == null ? null : ((Number) pid).longValue();
            String role = (String) row.get("role");
            long count = longValue(row.get("user_count"));
            result.computeIfAbsent(packageId, k -> new LinkedHashMap<>()).put(role, count);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RoleDistributionDto> getRoleDistribution(LocalDateTime start, LocalDateTime end, long totalUsers) {
        validateRange(start, end);

        Map<String, String> displayNames = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, Role::getDisplayName, (a, b) -> a));

        List<Tuple> rows = entityManager.createNativeQuery(
                "SELECT ur.primary_role AS role, COUNT(*) AS user_count FROM (" +
                        "SELECT u.id, " + PRIMARY_ROLE_SUBQUERY + " AS primary_role " +
                        "FROM users u WHERE u.deleted_at IS NULL" +
                        ") ur GROUP BY ur.primary_role ORDER BY user_count DESC", Tuple.class)
                .getResultList();

        List<RoleDistributionDto> result = new ArrayList<>();
        for (Tuple row : rows) {
            String role = (String) row.get("role");
            long count = ((Number) row.get("user_count")).longValue();
            result.add(new RoleDistributionDto(role, displayNames.getOrDefault(role, role), count,
                    totalUsers > 0 ? (double) count / totalUsers * 100 : 0.0));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ActiveInactiveDto> getStatusDistribution(LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);

        List<Tuple> rows = entityManager.createNativeQuery(
                "SELECT CASE WHEN is_active THEN 'active' ELSE 'inactive' END AS status, COUNT(*) AS count " +
                        "FROM users WHERE deleted_at IS NULL GROUP BY is_active", Tuple.class)
                .getResultList();

        long total = rows.stream().mapToLong(r -> longValue(r.get("count"))).sum();
        List<ActiveInactiveDto> result = new ArrayList<>();
        for (Tuple row : rows) {
            String status = (String) row.get("status");
            long count = longValue(row.get("count"));
            result.add(new ActiveInactiveDto(status, count, percent(count, total)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<TrendDto> getTrends(LocalDateTime start, LocalDateTime end, String granularity) {
        validateRange(start, end);
        String grain = ("month".equalsIgnoreCase(granularity)) ? "month" : "day";

        List<Tuple> rows = entityManager.createNativeQuery(
                "SELECT date_trunc(:grain, created_at) AS period, COUNT(*) AS user_count " +
                        "FROM users WHERE deleted_at IS NULL AND created_at BETWEEN :start AND :end " +
                        "GROUP BY 1 ORDER BY 1", Tuple.class)
                .setParameter("grain", grain)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        List<TrendDto> result = new ArrayList<>();
        for (Tuple row : rows) {
            java.sql.Timestamp ts = (java.sql.Timestamp) row.get("period");
            result.add(new TrendDto(
                    ts.toLocalDateTime().toString(),
                    longValue(row.get("user_count")),
                    longValue(row.get("user_count"))
            ));
        }
        return result;
    }

    private Long countNewUsersInPeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (periodStart == null || periodEnd == null) {
            return null;
        }
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND created_at BETWEEN :start AND :end")
                .setParameter("start", periodStart)
                .setParameter("end", periodEnd)
                .getSingleResult()).longValue();
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private double percent(long part, long total) {
        return total > 0 ? Math.round((double) part / total * 1000.0) / 10.0 : 0.0;
    }

    private long longValue(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
