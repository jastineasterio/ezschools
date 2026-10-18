package com.edumanage.resultapprovals.controller;

import com.edumanage.resultapprovals.dto.ResultApprovalDto;
import com.edumanage.resultapprovals.service.ResultApprovalService;
import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/school/result-approvals")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResultApprovalController {

    private final ResultApprovalService resultApprovalService;

    @GetMapping("/result/{resultId}")
    public ResponseEntity<ApiResponse<PageResponse<ResultApprovalDto>>> listByResult(@PathVariable Long resultId,
                                                                                     Pageable pageable) {
        var resultApprovals = resultApprovalService.listByResult(resultId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Result approvals fetched", resultApprovals, null, LocalDateTime.now(), "/api/school/result-approvals/result/" + resultId));
    }
}