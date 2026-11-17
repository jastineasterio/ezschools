package com.edumanage.resultsnotifications.integration;

import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ResultNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ExaminationRepository examinationRepository;

    private String tokenForSchool(Long schoolId) {
        var userDetails = User.withUsername("manager").password("").roles("MANAGER").build();
        return jwtService.generateAccessToken(userDetails, 100L, schoolId, List.of("MANAGER"));
    }

    @Test
    void managerRetrievesExaminationsForTheirSchool() throws Exception {
        mockMvc.perform(get("/school/results-notifications/examinations")
                        .header("Authorization", "Bearer " + tokenForSchool(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("GAUGING EXAMINATIONS"))
                .andExpect(jsonPath("$.data[0].schoolId").value(2));
    }

    @Test
    void emptyExaminationListReturns200Not500() throws Exception {
        mockMvc.perform(get("/school/results-notifications/examinations")
                        .header("Authorization", "Bearer " + tokenForSchool(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void schoolIsolationOtherSchoolExaminationsNeverAppear() throws Exception {
        Examination other = new Examination();
        other.setSchoolId(1L);
        other.setAcademicYearId(1L);
        other.setTypeId(1L);
        other.setName("Other School Exam");
        other.setStatus("DRAFT");
        examinationRepository.saveAndFlush(other);

        mockMvc.perform(get("/school/results-notifications/examinations")
                        .header("Authorization", "Bearer " + tokenForSchool(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[*].schoolId", everyItem(is(2))));

        mockMvc.perform(get("/school/results-notifications/examinations")
                        .header("Authorization", "Bearer " + tokenForSchool(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(other.getId()))
                .andExpect(jsonPath("$.data[0].schoolId").value(1L));
    }

    @Test
    void classesRetrievedForAuthenticatedSchool() throws Exception {
        mockMvc.perform(get("/school/results-notifications/classes")
                        .header("Authorization", "Bearer " + tokenForSchool(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].schoolId", everyItem(is(2))));
    }

    @Test
    void streamsRetrievedForSelectedClassAndSchool() throws Exception {
        mockMvc.perform(get("/school/results-notifications/streams")
                        .param("classId", "1")
                        .header("Authorization", "Bearer " + tokenForSchool(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("A"))
                .andExpect(jsonPath("$.data[0].schoolId").value(2L))
                .andExpect(jsonPath("$.data[0].classId").value(1L));
    }

    @Test
    void streamsEmptyWhenNoStreamForClass() throws Exception {
        mockMvc.perform(get("/school/results-notifications/streams")
                        .param("classId", "2")
                        .header("Authorization", "Bearer " + tokenForSchool(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void streamsDoesNotCrashWhenNoClassSelected() throws Exception {
        mockMvc.perform(get("/school/results-notifications/streams")
                        .param("classId", "999999")
                        .header("Authorization", "Bearer " + tokenForSchool(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
