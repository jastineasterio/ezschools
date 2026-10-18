package com.edumanage.sms.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.sms.dto.CreateSmsTemplateRequest;
import com.edumanage.sms.dto.SmsTemplateDto;
import com.edumanage.sms.service.SmsTemplateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/sms/templates")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SmsTemplateController {

    private final SmsTemplateService smsTemplateService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> listTemplates(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        var templates = smsTemplateService.list(PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS templates fetched", templates, null,
                LocalDateTime.now(), "/api/school/sms/templates"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SmsTemplateDto>> getTemplate(@PathVariable Long id) {
        var template = smsTemplateService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS template fetched", template, null,
                LocalDateTime.now(), "/api/school/sms/templates/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SmsTemplateDto>> createTemplate(@RequestBody CreateSmsTemplateRequest request) {
        var template = smsTemplateService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS template created", template, null,
                LocalDateTime.now(), "/api/school/sms/templates"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SmsTemplateDto>> updateTemplate(@PathVariable Long id,
                                                                      @RequestBody CreateSmsTemplateRequest request) {
        var template = smsTemplateService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS template updated", template, null,
                LocalDateTime.now(), "/api/school/sms/templates/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        smsTemplateService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS template deleted", null, null,
                LocalDateTime.now(), "/api/school/sms/templates/" + id));
    }
}
