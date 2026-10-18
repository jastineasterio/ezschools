package com.edumanage.sms.controller;

import com.edumanage.common.dto.ApiResponse;
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

    @GetMapping("/messages/status/{status}")
    public ResponseEntity<ApiResponse<?>> listMessagesByStatus(@PathVariable String status,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        var messages = smsMessageService.getMessagesByStatus(status, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(true, "SMS messages fetched", messages, null,
                LocalDateTime.now(), "/api/school/sms/messages/status/" + status));
    }
}
