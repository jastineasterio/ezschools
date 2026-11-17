package com.edumanage.sms.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.sms.dto.CreateSmsMessageRequest;
import com.edumanage.sms.dto.SmsMessageDto;
import com.edumanage.sms.service.SmsMessageService;
import com.edumanage.sms.service.SmsTemplateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/sms")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SmsMessageController {

    private final SmsMessageService smsMessageService;
    private final SmsTemplateService smsTemplateService;

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<?>> listMessages(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        var messages = smsMessageService.getMessages(PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS messages fetched", messages, null,
                LocalDateTime.now(), "/api/school/sms/messages"));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<SmsMessageDto>> sendSms(@RequestBody CreateSmsMessageRequest request) {
        var dto = smsMessageService.sendSms(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS queued for sending", dto, null,
                LocalDateTime.now(), "/api/school/sms/messages"));
    }

    @PostMapping("/messages/bulk")
    public ResponseEntity<ApiResponse<List<SmsMessageDto>>> sendBulkSms(@RequestBody CreateSmsMessageRequest request) {
        var dtos = smsMessageService.sendBulkSms(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulk SMS queued for sending", dtos, null,
                LocalDateTime.now(), "/api/school/sms/messages/bulk"));
    }

    @GetMapping("/messages/status/{status}")
    public ResponseEntity<ApiResponse<?>> listMessagesByStatus(@PathVariable String status,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        var messages = smsMessageService.getMessagesByStatus(status, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS messages fetched", messages, null,
                LocalDateTime.now(), "/api/school/sms/messages/status/" + status));
    }

    @GetMapping("/messages/{messageId}/status")
    public ResponseEntity<ApiResponse<SmsMessageDto>> checkMessageStatus(@PathVariable Long messageId) {
        var dto = smsMessageService.checkMessageStatus(messageId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Message status fetched", dto, null,
                LocalDateTime.now(), "/api/school/sms/messages/" + messageId + "/status"));
    }
}
