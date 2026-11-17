package com.edumanage.resultsnotifications.controller;

import com.edumanage.classes.entity.Class;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.examinations.dto.ExaminationDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationDeliveryDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationDto;
import com.edumanage.resultsnotifications.dto.ResultNotificationPreview;
import com.edumanage.resultsnotifications.dto.ResultNotificationRequest;
import com.edumanage.resultsnotifications.dto.ResultNotificationSummary;
import com.edumanage.resultsnotifications.dto.ResultNotificationTemplateDto;
import com.edumanage.resultsnotifications.dto.SaveResultNotificationTemplateRequest;
import com.edumanage.resultsnotifications.service.ResultNotificationService;
import com.edumanage.resultsnotifications.service.ResultNotificationTemplateService;
import com.edumanage.streams.dto.StreamDto;
import com.edumanage.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/results-notifications")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
@RequiredArgsConstructor
public class ResultNotificationController {

    private final ResultNotificationService resultNotificationService;
    private final ResultNotificationTemplateService resultNotificationTemplateService;

    @GetMapping("/examinations")
    public ResponseEntity<ApiResponse<List<ExaminationDto>>> listExaminations() {
        Long schoolId = TenantContext.requireTenantId();
        List<ExaminationDto> examinations = resultNotificationService.getExaminations(schoolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Examinations fetched", examinations, null,
                LocalDateTime.now(), "/api/school/results-notifications/examinations"));
    }

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<Class>>> listClasses() {
        Long schoolId = TenantContext.requireTenantId();
        List<Class> classes = resultNotificationService.getClasses(schoolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Classes fetched", classes, null,
                LocalDateTime.now(), "/api/school/results-notifications/classes"));
    }

    @GetMapping("/streams")
    public ResponseEntity<ApiResponse<List<StreamDto>>> listStreams(@RequestParam Long classId) {
        Long schoolId = TenantContext.requireTenantId();
        List<StreamDto> streams = resultNotificationService.getStreams(schoolId, classId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Streams fetched", streams, null,
                LocalDateTime.now(), "/api/school/results-notifications/streams"));
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<ResultNotificationPreview>> preview(
            @RequestParam Long examinationId,
            @RequestParam Long classId,
            @RequestParam(required = false) Long streamId,
            @RequestParam String recipientType) {
        Long schoolId = TenantContext.requireTenantId();
        ResultNotificationPreview preview = resultNotificationService.preview(
                schoolId, examinationId, classId, streamId, recipientType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Preview fetched", preview, null,
                LocalDateTime.now(), "/api/school/results-notifications/preview"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResultNotificationDto>> send(
            @Valid @RequestBody ResultNotificationRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        Long initiatedBy = com.edumanage.common.util.AuditAware.currentUserId();
        ResultNotificationDto dto = resultNotificationService.send(
                schoolId, request.getExaminationId(), request.getClassId(), request.getStreamId(),
                request.getRecipientType(), request.getIdempotencyKey(), initiatedBy);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results notification queued", dto, null,
                LocalDateTime.now(), "/api/school/results-notifications"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ResultNotificationDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long schoolId = TenantContext.requireTenantId();
        Page<ResultNotificationDto> dtos = resultNotificationService.list(
                schoolId, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(new ApiResponse<>(true, "Result notifications fetched", dtos, null,
                LocalDateTime.now(), "/api/school/results-notifications"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResultNotificationDto>> get(@PathVariable Long id) {
        Long schoolId = TenantContext.requireTenantId();
        ResultNotificationDto dto = resultNotificationService.getById(schoolId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result notification fetched", dto, null,
                LocalDateTime.now(), "/api/school/results-notifications/" + id));
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<ApiResponse<List<ResultNotificationDeliveryDto>>> deliveries(@PathVariable Long id) {
        Long schoolId = TenantContext.requireTenantId();
        List<ResultNotificationDeliveryDto> deliveries = resultNotificationService.getDeliveries(schoolId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery statuses fetched", deliveries, null,
                LocalDateTime.now(), "/api/school/results-notifications/" + id + "/deliveries"));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<ResultNotificationSummary>> summary(@PathVariable Long id) {
        Long schoolId = TenantContext.requireTenantId();
        ResultNotificationSummary summary = resultNotificationService.getSummary(schoolId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Summary fetched", summary, null,
                LocalDateTime.now(), "/api/school/results-notifications/" + id + "/summary"));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<ResultNotificationDto>> retry(@PathVariable Long id) {
        Long schoolId = TenantContext.requireTenantId();
        Long initiatedBy = com.edumanage.common.util.AuditAware.currentUserId();
        ResultNotificationDto dto = resultNotificationService.retryFailed(schoolId, id, initiatedBy);
        return ResponseEntity.ok(new ApiResponse<>(true, "Eligible failed deliveries retried", dto, null,
                LocalDateTime.now(), "/api/school/results-notifications/" + id + "/retry"));
    }

    @GetMapping("/template")
    public ResponseEntity<ApiResponse<ResultNotificationTemplateDto>> getTemplate() {
        Long schoolId = TenantContext.requireTenantId();
        ResultNotificationTemplateDto dto = resultNotificationTemplateService.getTemplate(schoolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results SMS template fetched", dto, null,
                LocalDateTime.now(), "/api/school/results-notifications/template"));
    }

    @PutMapping("/template")
    public ResponseEntity<ApiResponse<ResultNotificationTemplateDto>> saveTemplate(
            @Valid @RequestBody SaveResultNotificationTemplateRequest request) {
        Long schoolId = TenantContext.requireTenantId();
        Long updatedBy = com.edumanage.common.util.AuditAware.currentUserId();
        ResultNotificationTemplateDto dto = resultNotificationTemplateService.saveTemplate(
                schoolId, request.getSmsTemplate(), updatedBy);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results SMS template saved", dto, null,
                LocalDateTime.now(), "/api/school/results-notifications/template"));
    }

    @PostMapping("/template/restore-default")
    public ResponseEntity<ApiResponse<ResultNotificationTemplateDto>> restoreDefaultTemplate() {
        Long schoolId = TenantContext.requireTenantId();
        Long updatedBy = com.edumanage.common.util.AuditAware.currentUserId();
        ResultNotificationTemplateDto dto = resultNotificationTemplateService.restoreDefault(schoolId, updatedBy);
        return ResponseEntity.ok(new ApiResponse<>(true, "Results SMS template restored to default", dto, null,
                LocalDateTime.now(), "/api/school/results-notifications/template/restore-default"));
    }
}
