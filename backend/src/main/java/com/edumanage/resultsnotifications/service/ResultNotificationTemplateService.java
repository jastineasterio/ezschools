package com.edumanage.resultsnotifications.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.resultsnotifications.dto.PlaceholderInfo;
import com.edumanage.resultsnotifications.dto.ResultNotificationTemplateDto;
import com.edumanage.resultsnotifications.entity.ResultNotificationSetting;
import com.edumanage.resultsnotifications.repository.ResultNotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResultNotificationTemplateService {

    private static final String DEFAULT_TEMPLATE =
            "Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}";

    private static final int MAX_TEMPLATE_LENGTH = 2000;

    private static final Set<String> SUPPORTED_PLACEHOLDERS = Set.of(
            "student_name", "admission_number", "class_name", "stream_name",
            "examination_name", "subject_results", "school_name", "recipient_name"
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final ResultNotificationSettingRepository settingRepository;
    private final AuditLogService auditLogService;

    public ResultNotificationTemplateDto getTemplate(Long schoolId) {
        ResultNotificationSetting setting = settingRepository.findBySchoolId(schoolId).orElse(null);
        String template = (setting != null && setting.getSmsTemplate() != null)
                ? setting.getSmsTemplate()
                : DEFAULT_TEMPLATE;
        boolean isDefault = setting == null || setting.getSmsTemplate() == null || setting.getSmsTemplate().isBlank();
        return new ResultNotificationTemplateDto(
                schoolId, template, isDefault,
                setting != null ? setting.getUpdatedAt() : null,
                supportedPlaceholders());
    }

    @Transactional
    public ResultNotificationTemplateDto saveTemplate(Long schoolId, String smsTemplate, Long updatedBy) {
        List<String> errors = validate(smsTemplate);
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }

        ResultNotificationSetting setting = settingRepository.findBySchoolId(schoolId).orElse(new ResultNotificationSetting());
        setting.setSchoolId(schoolId);
        setting.setSmsTemplate(smsTemplate);
        setting.setUpdatedBy(updatedBy);
        settingRepository.save(setting);

        auditLogService.log("RESULT_NOTIFICATION_TEMPLATE_SAVE", "ResultNotificationSetting", setting.getId(),
                "Customized results SMS template for school " + schoolId, null, null, null);

        return getTemplate(schoolId);
    }

    @Transactional
    public ResultNotificationTemplateDto restoreDefault(Long schoolId, Long updatedBy) {
        settingRepository.findBySchoolId(schoolId).ifPresent(settingRepository::delete);

        auditLogService.log("RESULT_NOTIFICATION_TEMPLATE_RESTORE", "ResultNotificationSetting", null,
                "Restored default results SMS template for school " + schoolId, null, null, null);

        return getTemplate(schoolId);
    }

    /**
     * Shared rendering logic used by both the template preview and the actual SMS sending.
     * Empty subject results render as "(no results)" instead of misleading trailing punctuation.
     */
    public String render(String template, TemplateContext context) {
        if (template == null || template.isBlank()) {
            template = DEFAULT_TEMPLATE;
        }
        String result = template;
        result = replace(result, "{student_name}", context.studentName());
        result = replace(result, "{admission_number}", context.admissionNumber());
        result = replace(result, "{class_name}", context.className());
        result = replace(result, "{stream_name}", context.streamName());
        result = replace(result, "{examination_name}", context.examinationName());
        String subjectResults = context.subjectResults();
        if (subjectResults == null || subjectResults.isBlank()) {
            subjectResults = "(no results)";
        }
        result = replace(result, "{subject_results}", subjectResults);
        result = replace(result, "{school_name}", context.schoolName());
        result = replace(result, "{recipient_name}", context.recipientName());
        return result;
    }

    public List<PlaceholderInfo> supportedPlaceholders() {
        return List.of(
                new PlaceholderInfo("student_name", "Student's full name", "John Doe"),
                new PlaceholderInfo("admission_number", "Student's admission number", "ADM001"),
                new PlaceholderInfo("class_name", "Student's class", "Form 1"),
                new PlaceholderInfo("stream_name", "Student's stream, if any", "A"),
                new PlaceholderInfo("examination_name", "Examination name", "GAUGING EXAMINATIONS"),
                new PlaceholderInfo("subject_results", "Subject marks and grades", "Maths 78/100 (B)"),
                new PlaceholderInfo("school_name", "School name", "MJ School"),
                new PlaceholderInfo("recipient_name", "Recipient's name (student or parent/guardian)", "Jane Doe")
        );
    }

    /**
     * Validates a customized template. Rejects blank templates, overly long templates,
     * and unknown placeholders. Unicode (including Kiswahili) text is preserved.
     */
    public List<String> validate(String template) {
        List<String> errors = new ArrayList<>();
        if (template == null || template.isBlank()) {
            errors.add("Template is required");
            return errors;
        }
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            errors.add("Template is too long (maximum " + MAX_TEMPLATE_LENGTH + " characters)");
        }

        Set<String> unknown = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            if (!SUPPORTED_PLACEHOLDERS.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            errors.add("Unsupported placeholders: " + String.join(", ", unknown)
                    + ". Supported: " + String.join(", ", SUPPORTED_PLACEHOLDERS));
        }
        return errors;
    }

    private static String replace(String result, String placeholder, String value) {
        return result.replace(placeholder, value == null ? "" : value);
    }

    public record TemplateContext(
            String studentName,
            String admissionNumber,
            String className,
            String streamName,
            String examinationName,
            String subjectResults,
            String schoolName,
            String recipientName
    ) {
    }
}
