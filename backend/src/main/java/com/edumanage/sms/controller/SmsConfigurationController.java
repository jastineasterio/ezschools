package com.edumanage.sms.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.sms.dto.CreateSmsConfigurationRequest;
import com.edumanage.sms.dto.SmsConfigurationDto;
import com.edumanage.sms.service.SmsConfigurationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/sms/configurations")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class SmsConfigurationController {

    private final SmsConfigurationService smsConfigurationService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllConfigurations(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        var configs = smsConfigurationService.getAllConfigurations(PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configurations fetched", configs, null,
                LocalDateTime.now(), "/api/platform/sms/configurations"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SmsConfigurationDto>> getConfiguration(@PathVariable Long id) {
        var config = smsConfigurationService.getConfiguration(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration fetched", config, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SmsConfigurationDto>> createConfiguration(
            @RequestBody CreateSmsConfigurationRequest request) {
        var config = smsConfigurationService.createConfiguration(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration created", config, null,
                LocalDateTime.now(), "/api/platform/sms/configurations"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SmsConfigurationDto>> updateConfiguration(
            @PathVariable Long id,
            @RequestBody CreateSmsConfigurationRequest request) {
        var config = smsConfigurationService.updateConfiguration(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration updated", config, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfiguration(@PathVariable Long id) {
        smsConfigurationService.deleteConfiguration(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS configuration deleted", null, null,
                LocalDateTime.now(), "/api/platform/sms/configurations/" + id));
    }
}
