package com.edumanage.sms.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.sms.dto.CreateSmsConfigurationRequest;
import com.edumanage.sms.dto.SmsConfigurationDto;
import com.edumanage.sms.dto.UpdateSmsConfigurationRequest;
import com.edumanage.sms.service.SmsConfigurationService;
import com.edumanage.schools.repository.SchoolRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/platform/sms/configurations")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class SmsConfigurationController {

    private final SmsConfigurationService smsConfigurationService;
    private final SchoolRepository schoolRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllConfigurations(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        var configs = smsConfigurationService.getAllConfigurations(PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configurations fetched", configs, null,
                LocalDateTime.now(), "/api/platform/sms/configurations"));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<ApiResponse<PageResponse<SmsConfigurationDto>>> getConfigurationsForSchool(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
        var configs = smsConfigurationService.getConfigurationsForSchool(schoolId, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configurations fetched", configs, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/school/" + schoolId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SmsConfigurationDto>> getConfiguration(@PathVariable Long id) {
        var config = smsConfigurationService.getConfiguration(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration fetched", config, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SmsConfigurationDto>> createConfiguration(
            @RequestBody @Valid CreateSmsConfigurationRequest request) {
        var config = smsConfigurationService.createConfiguration(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration created", config, null,
                LocalDateTime.now(), "/api/platform/sms/configurations"));
    }

    @PostMapping("/sender-ids")
    public ResponseEntity<ApiResponse<List<String>>> fetchSenderIds(
            @RequestBody @Valid FetchSenderIdsRequest request) {
        List<String> senderIds = smsConfigurationService.getApprovedSenderIdsByApiKey(request.getApiKey());
        return ResponseEntity.ok(new ApiResponse<>(true, "Approved sender IDs fetched", senderIds, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/sender-ids"));
    }

    @GetMapping("/{id}/sender-ids")
    public ResponseEntity<ApiResponse<List<String>>> getSenderIds(@PathVariable Long id) {
        var config = smsConfigurationService.findRawById(id);
        if (config.getDeletedAt() != null) {
            throw new ResourceNotFoundException("SMS configuration not found");
        }
        List<String> senderIds = smsConfigurationService.getApprovedSenderIds(config.getSchoolId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Approved sender IDs fetched", senderIds, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id + "/sender-ids"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SmsConfigurationDto>> updateConfiguration(
            @PathVariable Long id,
            @RequestBody @Valid UpdateSmsConfigurationRequest request) {
        var config = smsConfigurationService.updateConfiguration(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration updated", config, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<SmsConfigurationService.SmsTestResult>> testConfiguration(
            @PathVariable Long id,
            @RequestBody TestConfigurationRequest request) {
        var config = smsConfigurationService.findRawById(id);
        if (config.getDeletedAt() != null) {
            throw new ResourceNotFoundException("SMS configuration not found");
        }
        SmsConfigurationService.SmsTestResult result = smsConfigurationService.testConfiguration(config.getSchoolId(),
                request.getRecipient());
        return ResponseEntity.ok(new ApiResponse<>(result.success(),
                result.success() ? "Connection successful" : "Test connection failed: " + result.failureCode(), result, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id + "/test"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfiguration(@PathVariable Long id) {
        smsConfigurationService.deleteConfiguration(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration deleted", null, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id));
    }

    public static class TestConfigurationRequest {
        private String recipient;

        public String getRecipient() {
            return recipient;
        }

        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }
    }

    public static class FetchSenderIdsRequest {
        @jakarta.validation.constraints.NotBlank(message = "API key is required")
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
