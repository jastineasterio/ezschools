package com.edumanage.certificates.controller;

import com.edumanage.certificates.dto.CertificateDto;
import com.edumanage.certificates.dto.CertificateRequest;
import com.edumanage.certificates.dto.EligibleStudentDto;
import com.edumanage.certificates.service.CertificateService;
import com.edumanage.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/certificates")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService certificateService;

    @GetMapping("/eligible")
    public ResponseEntity<ApiResponse<List<EligibleStudentDto>>> eligible(@RequestParam Long academicYearId,
                                                                          @RequestParam Long classId,
                                                                          @RequestParam(required = false) Long streamId,
                                                                          @RequestParam Long subjectId) {
        var data = certificateService.eligible(academicYearId, classId, streamId, subjectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Eligible students fetched", data, null,
                LocalDateTime.now(), "/api/school/certificates/eligible"));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<EligibleStudentDto>>> students(@RequestParam Long academicYearId,
                                                                          @RequestParam Long classId,
                                                                          @RequestParam(required = false) Long streamId) {
        var data = certificateService.studentsInScope(academicYearId, classId, streamId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrolled students fetched", data, null,
                LocalDateTime.now(), "/api/school/certificates/students"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificateDto>>> list() {
        var data = certificateService.listForManager();
        return ResponseEntity.ok(new ApiResponse<>(true, "Certificates fetched", data, null,
                LocalDateTime.now(), "/api/school/certificates"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CertificateDto>> generate(@Valid @RequestBody CertificateRequest request) {
        var data = certificateService.generate(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Certificate generated", data, null,
                LocalDateTime.now(), "/api/school/certificates"));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<CertificateDto>> release(@PathVariable Long id) {
        var data = certificateService.release(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Certificate released", data, null,
                LocalDateTime.now(), "/api/school/certificates/" + id + "/release"));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<CertificateDto>> revoke(@PathVariable Long id, @RequestParam String reason) {
        var data = certificateService.revoke(id, reason);
        return ResponseEntity.ok(new ApiResponse<>(true, "Certificate revoked", data, null,
                LocalDateTime.now(), "/api/school/certificates/" + id + "/revoke"));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<CertificateDto>>> mine() {
        var data = certificateService.listForCurrentStudent();
        return ResponseEntity.ok(new ApiResponse<>(true, "Your certificates fetched", data, null,
                LocalDateTime.now(), "/api/school/certificates/mine"));
    }

    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + id + ".pdf\"")
                .body(certificateService.document(id));
    }
}
