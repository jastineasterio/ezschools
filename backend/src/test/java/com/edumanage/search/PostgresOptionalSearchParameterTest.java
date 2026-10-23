package com.edumanage.search;

import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.audit.repository.AuditLogRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.departments.repository.DepartmentRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresOptionalSearchParameterTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private StreamRepository streamRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void pagedSearchesAcceptNullOptionalTextFiltersOnPostgres() {
        var pageable = PageRequest.of(0, 10);

        assertDoesNotThrow(() -> academicYearRepository.search(2L, null, pageable));
        assertDoesNotThrow(() -> streamRepository.search(2L, null, pageable));
        assertDoesNotThrow(() -> classRepository.search(2L, null, pageable));
        assertDoesNotThrow(() -> departmentRepository.search(2L, null, pageable));
        assertDoesNotThrow(() -> subjectRepository.search(2L, null, pageable));
        assertDoesNotThrow(() -> userRepository.findSchoolUsers(2L, null, null, null, pageable));
        assertDoesNotThrow(() -> auditLogRepository.findFilteredBySchoolId(2L, null, null, null, pageable));
        assertDoesNotThrow(() -> auditLogRepository.findFiltered(null, null, null, pageable));
    }
}
