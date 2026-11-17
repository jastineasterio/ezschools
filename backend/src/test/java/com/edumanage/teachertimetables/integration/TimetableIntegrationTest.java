package com.edumanage.teachertimetables.integration;

import com.edumanage.security.JwtService;
import com.edumanage.teachertimetables.entity.TeacherTimetable;
import com.edumanage.teachertimetables.repository.TeacherTimetableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class TimetableIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private TeacherTimetableRepository timetableRepository;

    private String managerToken(Long schoolId) {
        var userDetails = User.withUsername("manager").password("").roles("MANAGER").build();
        return jwtService.generateAccessToken(userDetails, 100L, schoolId, List.of("MANAGER"));
    }

    @Test
    void managerCreatesValidLessonFromExistingTeacherAssignment() throws Exception {
        var body = """
                {"teacherAssignmentId":1,"dayOfWeek":2,"startTime":"09:00","endTime":"10:00","room":"UNIQUE-1"}
                """;
        mockMvc.perform(post("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teacherAssignmentId").value(1))
                .andExpect(jsonPath("$.data.dayOfWeek").value(2))
                .andExpect(jsonPath("$.data.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("10:00:00"))
                .andExpect(jsonPath("$.data.room").value("UNIQUE-1"))
                .andExpect(jsonPath("$.data.className").value("form 1"))
                .andExpect(jsonPath("$.data.streamName").value("A"))
                .andExpect(jsonPath("$.data.subjectName").value("kiswahili"));
    }

    @Test
    void getReturnsCreatedLesson() throws Exception {
        timetableRepository.deleteBySchoolId(2L);
        TeacherTimetable t = new TeacherTimetable();
        t.setSchoolId(2L);
        t.setTeacherAssignmentId(1L);
        t.setDayOfWeek(3);
        t.setStartTime(LocalTime.of(10, 0));
        t.setEndTime(LocalTime.of(11, 0));
        t.setRoom("GET-LESSON");
        timetableRepository.saveAndFlush(t);

        mockMvc.perform(get("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].room").value("GET-LESSON"))
                .andExpect(jsonPath("$.data[0].className").value("form 1"))
                .andExpect(jsonPath("$.data[0].subjectName").value("kiswahili"));
    }

    @Test
    void getReturnsEmptyListWhenNoLessons() throws Exception {
        timetableRepository.deleteBySchoolId(2L);

        mockMvc.perform(get("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void invalidAssignmentIsRejectedSafely() throws Exception {
        var body = """
                {"teacherAssignmentId":999999,"dayOfWeek":1,"startTime":"04:54","endTime":"05:22","room":"12"}
                """;
        mockMvc.perform(post("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void startTimeAfterEndTimeIsRejected() throws Exception {
        var body = """
                {"teacherAssignmentId":1,"dayOfWeek":1,"startTime":"05:22","endTime":"04:54","room":"12"}
                """;
        mockMvc.perform(post("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void schoolCannotAccessOtherSchoolTimetable() throws Exception {
        timetableRepository.deleteBySchoolId(2L);
        TeacherTimetable other = new TeacherTimetable();
        other.setSchoolId(1L);
        other.setTeacherAssignmentId(1L);
        other.setDayOfWeek(1);
        other.setStartTime(LocalTime.of(8, 0));
        other.setEndTime(LocalTime.of(9, 0));
        other.setRoom("5");
        timetableRepository.saveAndFlush(other);

        mockMvc.perform(get("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void conflictingLessonIsRejected() throws Exception {
        timetableRepository.deleteBySchoolId(2L);
        TeacherTimetable existing = new TeacherTimetable();
        existing.setSchoolId(2L);
        existing.setTeacherAssignmentId(1L);
        existing.setDayOfWeek(1);
        existing.setStartTime(LocalTime.of(4, 54));
        existing.setEndTime(LocalTime.of(5, 22));
        existing.setRoom("EXISTING");
        timetableRepository.saveAndFlush(existing);

        var conflictBody = """
                {"teacherAssignmentId":1,"dayOfWeek":1,"startTime":"05:00","endTime":"05:10","room":"13"}
                """;
        mockMvc.perform(post("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L))
                        .contentType("application/json")
                        .content(conflictBody))
                .andExpect(status().isConflict());
    }

    @Test
    void dtoSerializationWorksWithOpenInViewDisabled() throws Exception {
        timetableRepository.deleteBySchoolId(2L);
        TeacherTimetable t = new TeacherTimetable();
        t.setSchoolId(2L);
        t.setTeacherAssignmentId(1L);
        t.setDayOfWeek(3);
        t.setStartTime(LocalTime.of(10, 0));
        t.setEndTime(LocalTime.of(11, 0));
        t.setRoom("DTO-LESSON");
        timetableRepository.saveAndFlush(t);

        mockMvc.perform(get("/school/timetables")
                        .header("Authorization", "Bearer " + managerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].room").value("DTO-LESSON"))
                .andExpect(jsonPath("$.data[0].className").value("form 1"))
                .andExpect(jsonPath("$.data[0].streamName").value("A"))
                .andExpect(jsonPath("$.data[0].subjectName").value("kiswahili"));
    }
}
