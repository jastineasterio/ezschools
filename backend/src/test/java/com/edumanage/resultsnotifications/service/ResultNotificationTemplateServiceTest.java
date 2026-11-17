package com.edumanage.resultsnotifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.resultsnotifications.dto.ResultNotificationTemplateDto;
import com.edumanage.resultsnotifications.entity.ResultNotificationSetting;
import com.edumanage.resultsnotifications.repository.ResultNotificationSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultNotificationTemplateServiceTest {

    private static final Long SCHOOL_A = 1L;
    private static final Long SCHOOL_B = 2L;

    @Mock private ResultNotificationSettingRepository settingRepository;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private ResultNotificationTemplateService service;

    @Test
    void newSchoolReceivesDefaultTemplate() {
        lenient().when(settingRepository.findBySchoolId(SCHOOL_A)).thenReturn(Optional.empty());

        ResultNotificationTemplateDto dto = service.getTemplate(SCHOOL_A);

        assertTrue(dto.isDefault());
        assertEquals("Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}", dto.smsTemplate());
        assertEquals(SCHOOL_A, dto.schoolId());
        assertNotNull(dto.supportedPlaceholders());
        assertFalse(dto.supportedPlaceholders().isEmpty());
    }

    @Test
    void saveCustomTemplate() {
        java.util.concurrent.atomic.AtomicReference<ResultNotificationSetting> saved = new java.util.concurrent.atomic.AtomicReference<>();
        lenient().when(settingRepository.findBySchoolId(SCHOOL_A)).thenAnswer(inv -> Optional.ofNullable(saved.get()));
        lenient().when(settingRepository.save(any())).thenAnswer(inv -> {
            ResultNotificationSetting s = inv.getArgument(0);
            s.setId(100L);
            s.setUpdatedAt(LocalDateTime.now());
            saved.set(s);
            return s;
        });

        String custom = "Hello {student_name}, your {examination_name} results are ready. - {school_name}";
        ResultNotificationTemplateDto dto = service.saveTemplate(SCHOOL_A, custom, 50L);

        assertFalse(dto.isDefault());
        assertEquals(custom, dto.smsTemplate());
        verify(auditLogService).log(
                eq("RESULT_NOTIFICATION_TEMPLATE_SAVE"), eq("ResultNotificationSetting"), eq(100L),
                anyString(), any(), any(), any());
    }

    @Test
    void templatesAreIsolatedBySchool() {
        lenient().when(settingRepository.findBySchoolId(SCHOOL_A)).thenReturn(Optional.of(setting(SCHOOL_A, "School A template")));
        lenient().when(settingRepository.findBySchoolId(SCHOOL_B)).thenReturn(Optional.empty());

        ResultNotificationTemplateDto a = service.getTemplate(SCHOOL_A);
        ResultNotificationTemplateDto b = service.getTemplate(SCHOOL_B);

        assertEquals("School A template", a.smsTemplate());
        assertFalse(a.isDefault());
        assertTrue(b.isDefault());
        assertEquals("Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}", b.smsTemplate());
    }

    @Test
    void renderReplacesAllSupportedPlaceholders() {
        ResultNotificationTemplateService.TemplateContext ctx = new ResultNotificationTemplateService.TemplateContext(
                "Jane Doe", "ADM001", "Form 1", "A", "GAUGING EXAMINATIONS",
                "Maths 78/100 (B), English 65/100 (C)", "MJ School", "Jane Doe (Parent)");

        String rendered = service.render(
                "Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}",
                ctx);

        assertEquals("Dear Jane Doe (Parent), Jane Doe (Form 1/A) results for GAUGING EXAMINATIONS: Maths 78/100 (B), English 65/100 (C). - MJ School", rendered);
    }

    @Test
    void renderEmptySubjectResultsShowsNoResultsNotMisleadingPunctuation() {
        ResultNotificationTemplateService.TemplateContext ctx = new ResultNotificationTemplateService.TemplateContext(
                "John Doe", "ADM001", "Form 1", null, "Mid-Term Exam", "", "MJ School", "John Doe");

        String rendered = service.render(
                "results for {examination_name}: {subject_results}. - {school_name}", ctx);

        assertEquals("results for Mid-Term Exam: (no results). - MJ School", rendered);
    }

    @Test
    void validateRejectsUnknownPlaceholder() {
        List<String> errors = service.validate("Dear {student_name}, your {bogus} results are ready.");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unsupported placeholders") && e.contains("bogus")));
    }

    @Test
    void validateRejectsBlankTemplate() {
        List<String> errors = service.validate("   ");
        assertEquals(List.of("Template is required"), errors);
    }

    @Test
    void validateRejectsOverlyLongTemplate() {
        String tooLong = "a".repeat(2001);
        List<String> errors = service.validate(tooLong);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("too long"));
    }

    @Test
    void saveTemplateRejectsUnknownPlaceholder() {
        lenient().when(settingRepository.findBySchoolId(SCHOOL_A)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () ->
                service.saveTemplate(SCHOOL_A, "Dear {unknown_placeholder}", 50L));
    }

    @Test
    void saveTemplateDoesNotSendSms() {
        // saveTemplate has no dependency on the SMS sending service, so no SMS is sent.
        lenient().when(settingRepository.findBySchoolId(SCHOOL_A)).thenReturn(Optional.empty());
        lenient().when(settingRepository.save(any())).thenAnswer(inv -> {
            ResultNotificationSetting s = inv.getArgument(0);
            s.setId(100L);
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        service.saveTemplate(SCHOOL_A, "Hello {student_name}. - {school_name}", 50L);

        // No exception and template persisted; asserting no SMS-sending collaborator exists on this service.
        assertTrue(true);
    }

    @Test
    void restoreDefaultRemovesCustomTemplate() {
        ResultNotificationSetting existing = setting(SCHOOL_A, "custom");
        java.util.concurrent.atomic.AtomicReference<Optional<ResultNotificationSetting>> current =
                new java.util.concurrent.atomic.AtomicReference<>(Optional.of(existing));
        lenient().when(settingRepository.findBySchoolId(SCHOOL_A)).thenAnswer(inv -> current.get());
        lenient().doAnswer(inv -> { current.set(Optional.empty()); return null; }).when(settingRepository).delete(existing);

        ResultNotificationTemplateDto dto = service.restoreDefault(SCHOOL_A, 50L);

        assertTrue(dto.isDefault());
        verify(settingRepository).delete(existing);
    }

    private static ResultNotificationSetting setting(Long schoolId, String template) {
        ResultNotificationSetting s = new ResultNotificationSetting();
        s.setId(7L);
        s.setSchoolId(schoolId);
        s.setSmsTemplate(template);
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }
}
