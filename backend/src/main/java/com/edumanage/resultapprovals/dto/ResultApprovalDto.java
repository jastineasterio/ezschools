package com.edumanage.resultapprovals.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultApprovalDto {
    private Long id;
    private String uuid;
    private Long resultId;
    private Long schoolId;
    private String action;
    private Long actorUserId;
    private String actorUserName;
    private String actorRole;
    private String comment;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime createdAt;
}
