package com.edumanage.resultsnotifications.controller;

import com.edumanage.classes.entity.Class;
import com.edumanage.examinations.dto.ExaminationDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationPreview;
import com.edumanage.resultsnotifications.dto.ResultNotificationRequest;
import com.edumanage.resultsnotifications.dto.ResultNotificationSummary;
import com.edumanage.resultsnotifications.dto.ResultNotificationTemplateDto;
import com.edumanage.resultsnotifications.service.ResultNotificationService;
import com.edumanage.resultsnotifications.service.ResultNotificationTemplateService;
import com.edumanage.exception.GlobalExceptionHandler;
import com.edumanage.streams.dto.StreamDto;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ResultNotificationControllerTest {

    private ResultNotificationService service;
    private ResultNotificationTemplateService templateService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        service = mock(ResultNotificationService.class);
        templateService = mock(ResultNotificationTemplateService.class);
        when(templateService.getTemplate(1L)).thenReturn(new ResultNotificationTemplateDto(1L,
                "Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}",
                true, null, List.of()));
        mockMvc = standaloneSetup(new ResultNotificationController(service, templateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listExaminationsReturnsSchoolExaminations() throws Exception {
        ExaminationDto exam = new ExaminationDto();
        exam.setId(10L);
        exam.setSchoolId(1L);
        exam.setName("Mid-Term");
        when(service.getExaminations(1L)).thenReturn(List.of(exam));

        mockMvc.perform(get("/school/results-notifications/examinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void listClassesReturnsSchoolClasses() throws Exception {
        Class clazz = new Class();
        clazz.setId(20L);
        clazz.setSchoolId(1L);
        clazz.setName("Form 1");
        when(service.getClasses(1L)).thenReturn(List.of(clazz));

        mockMvc.perform(get("/school/results-notifications/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(20));
    }

    @Test
    void listStreamsReturnsStreamsForClass() throws Exception {
        StreamDto stream = new StreamDto();
        stream.setId(30L);
        stream.setSchoolId(1L);
        stream.setClassId(20L);
        stream.setName("A");
        when(service.getStreams(1L, 20L)).thenReturn(List.of(stream));

        mockMvc.perform(get("/school/results-notifications/streams")
                        .param("classId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(30));
    }

    @Test
    void previewReturnsEligibleCount() throws Exception {
        ResultNotificationPreview preview = new ResultNotificationPreview(
                10L, "Mid-Term", 20L, "Form 1", 30L, "A",
                "STUDENTS", 5, 0, List.of(), List.of(),
                "Sample message", true, null);
        when(service.preview(1L, 10L, 20L, 30L, "STUDENTS")).thenReturn(preview);

        mockMvc.perform(get("/school/results-notifications/preview")
                        .param("examinationId", "10")
                        .param("classId", "20")
                        .param("streamId", "30")
                        .param("recipientType", "STUDENTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligibleCount").value(5))
                .andExpect(jsonPath("$.data.canSend").value(true));
    }

    @Test
    void sendReturnsNotificationDto() throws Exception {
        ResultNotificationDto dto = new ResultNotificationDto(
                500L, "uuid", 1L, 10L, 20L, 30L,
                "STUDENTS", "idempotency-key", "Title",
                5, 0, 5, 0, 0, "PENDING", 10L, LocalDateTime.now());
        when(service.send(any(), any(), any(), any(),
                anyString(), anyString(), any())).thenReturn(dto);

        mockMvc.perform(post("/school/results-notifications")
                        .contentType("application/json")
                        .content("{\"examinationId\":10,\"classId\":20,\"streamId\":30,\"recipientType\":\"STUDENTS\",\"idempotencyKey\":\"idempotency-key\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(500));
    }

    @Test
    void listReturnsPagedNotifications() throws Exception {
        ResultNotificationDto dto = new ResultNotificationDto(
                1L, "uuid", 1L, 10L, 20L, 30L,
                "STUDENTS", "key", "Title",
                5, 0, 5, 0, 0, "PENDING", 10L, LocalDateTime.now());
        Page<ResultNotificationDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(service.list(1L, PageRequest.of(0, 20))).thenReturn(page);

        mockMvc.perform(get("/school/results-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void summaryReturnsDeliveryCounts() throws Exception {
        ResultNotificationSummary summary = new ResultNotificationSummary(5, 0, 5, 0, 0);
        when(service.getSummary(1L, 500L)).thenReturn(summary);

        mockMvc.perform(get("/school/results-notifications/500/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecipients").value(5))
                .andExpect(jsonPath("$.data.successfulCount").value(5));
    }

    @Test
    void sendValidationFailsOnMissingFields() throws Exception {
        mockMvc.perform(post("/school/results-notifications")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTemplateReturnsDefaultForNewSchool() throws Exception {
        when(templateService.getTemplate(1L)).thenReturn(new ResultNotificationTemplateDto(1L,
                "Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}",
                true, null, List.of()));

        mockMvc.perform(get("/school/results-notifications/template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andExpect(jsonPath("$.data.smsTemplate").value("Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}"));
    }

    @Test
    void saveTemplateSavesCustomTemplate() throws Exception {
        ResultNotificationTemplateDto saved = new ResultNotificationTemplateDto(1L,
                "Hello {student_name}. - {school_name}", false, LocalDateTime.now(), List.of());
        when(templateService.saveTemplate(eq(1L), anyString(), isNull())).thenReturn(saved);

        mockMvc.perform(put("/school/results-notifications/template")
                        .contentType("application/json")
                        .content("{\"smsTemplate\":\"Hello {student_name}. - {school_name}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.smsTemplate").value("Hello {student_name}. - {school_name}"));
    }

    @Test
    void saveTemplateRejectsUnknownPlaceholder() throws Exception {
        when(templateService.saveTemplate(eq(1L), anyString(), isNull()))
                .thenThrow(new com.edumanage.exception.BadRequestException("Unsupported placeholders: bogus"));

        mockMvc.perform(put("/school/results-notifications/template")
                        .contentType("application/json")
                        .content("{\"smsTemplate\":\"Dear {bogus}\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restoreDefaultRestoresDefaultTemplate() throws Exception {
        ResultNotificationTemplateDto def = new ResultNotificationTemplateDto(1L,
                "Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}",
                true, null, List.of());
        when(templateService.restoreDefault(eq(1L), isNull())).thenReturn(def);

        mockMvc.perform(post("/school/results-notifications/template/restore-default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }
}
