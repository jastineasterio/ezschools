package com.edumanage.certificates.controller;

import com.edumanage.certificates.dto.CertificateVerificationDto;
import com.edumanage.certificates.service.CertificateService;
import com.edumanage.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/public/certificates")
@RequiredArgsConstructor
public class CertificateVerificationController {
    private final CertificateService certificateService;

    @GetMapping("/verify/{verificationCode}")
    public ResponseEntity<ApiResponse<CertificateVerificationDto>> verify(@PathVariable String verificationCode) {
        var data = certificateService.verify(verificationCode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Certificate verification completed", data, null,
                LocalDateTime.now(), "/api/public/certificates/verify/" + verificationCode));
    }
}
