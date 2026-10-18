package com.edumanage.resultapprovals.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.resultapprovals.dto.ResultApprovalDto;
import com.edumanage.resultapprovals.entity.ResultApproval;
import com.edumanage.resultapprovals.repository.ResultApprovalRepository;
import com.edumanage.results.repository.ResultRepository;
import com.edumanage.users.entity.User;
import com.edumanage.users.repository.UserRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultApprovalService {

    private final ResultApprovalRepository resultApprovalRepository;
    private final ResultRepository resultRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ResultApprovalService(ResultApprovalRepository resultApprovalRepository,
                                 ResultRepository resultRepository,
                                 UserRepository userRepository,
                                 AuditLogService auditLogService) {
        this.resultApprovalRepository = resultApprovalRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<ResultApprovalDto> listByResult(Long resultId, Pageable pageable) {
        var result = resultRepository.findById(resultId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Page<ResultApproval> page = resultApprovalRepository.findByResultId(resultId, pageable);
        return PageResponse.from(page.map(approval -> {
            String actorUserName = userRepository.findById(approval.getActorUserId())
                    .map(user -> user.getFirstName() + " " + user.getLastName())
                    .orElse(null);
            return mapToDto(approval, actorUserName);
        }));
    }

    @Transactional
    public ResultApprovalDto create(Long resultId, String action, String actorRole, String comment,
                                    String oldStatus, String newStatus) {
        var result = resultRepository.findById(resultId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(result.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        var currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long actorUserId = null;
        if (currentUser != null && currentUser.isAuthenticated()) {
            var principal = currentUser.getPrincipal();
            if (principal instanceof com.edumanage.security.CustomUserDetails) {
                actorUserId = ((com.edumanage.security.CustomUserDetails) principal).id();
            }
        }

        if (actorUserId == null) {
            throw new ForbiddenException("User not authenticated");
        }

        ResultApproval approval = new ResultApproval();
        approval.setResultId(resultId);
        approval.setSchoolId(result.getSchoolId());
        approval.setAction(action);
        approval.setActorUserId(actorUserId);
        approval.setActorRole(actorRole);
        approval.setComment(comment);
        approval.setOldStatus(oldStatus);
        approval.setNewStatus(newStatus);

        resultApprovalRepository.save(approval);

        auditLogService.log("RESULT_APPROVAL_CREATE", "ResultApproval", approval.getId(),
                "Created result approval for result " + resultId, null, null, null);

        String actorUserName = userRepository.findById(actorUserId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);

        return mapToDto(approval, actorUserName);
    }

    private ResultApprovalDto mapToDto(ResultApproval entity, String actorUserName) {
        ResultApprovalDto dto = new ResultApprovalDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setResultId(entity.getResultId());
        dto.setSchoolId(entity.getSchoolId());
        dto.setAction(entity.getAction());
        dto.setActorUserId(entity.getActorUserId());
        dto.setActorUserName(actorUserName);
        dto.setActorRole(entity.getActorRole());
        dto.setComment(entity.getComment());
        dto.setOldStatus(entity.getOldStatus());
        dto.setNewStatus(entity.getNewStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
